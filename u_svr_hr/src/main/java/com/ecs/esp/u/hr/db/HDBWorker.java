package com.ecs.esp.u.hr.db;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.proc.EThreadWorkerPoolDB2;
import com.ecs.esp.u.com.define.CommonConstants;
import com.ecs.esp.u.hr.db.comps.DataSource;
import com.ecs.esp.u.hr.db.data.DataAlert;
import com.ecs.esp.u.hr.db.data.DataPhoneDo;
import com.ecs.esp.u.hr.db.data.DataUser;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.ConfCommon;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.msg.rest.custom.RESTMessage;
import com.ecs.msg.rest.handler.ECSMessageRest;
import io.netty.handler.codec.http.HttpMethod;

/******************************************************
 * Local/Remote 이외 DB 처리
 * 사용자 내선번호 갱신
 * 메신저 Alert 처리 : 내선중복 알림
 * PhoneDo 처리
 * 기간계 역방향 내선번호 갱신
 *
 * 
 ******************************************************/
public class HDBWorker extends EThreadWorkerPoolDB2 {
	private final String 		queryFile;
	private DataSource 	dbData;

	/**
	 * 생성자
	 * @param threadID ThreadID
	 * @param queryFile 질의문 파일
	 */
	public HDBWorker(int threadID, String queryFile) {
		super(threadID);
		this.queryFile = queryFile;
	}

	/**
	 * DB 접속/재접속/초기화/테이블 생성 등
	 * @return 성공/실패 여부
	 */
	public boolean Start() {
		try {
			EDBM dbm = DBConnect();
			if (dbm != null) {
				dbData = new DataSource(this.queryFile);
				if(dbData.Init(dbm)){
					UtilLog.t(getClass(), "Start Init.");
				}
				UtilLog.i(getClass(), "[Start] " + dbm.GetConnString());
			}
			return super.Start(Conf.getInstance().DB_WORKER_ETHREADS());
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return false;
	}

	/**
	 * 정지
	 * @return 성공/실패 여부
	 */
	public boolean Stop() {
		super.stop();
		super.DBClose();
		return true;
	}

	/**
	 * DB 접속
	 * @return EDBM 객체
	 */
	private EDBM DBConnect() {
		try {
			super.Config(this.queryFile, ConfCommon.getInstance());
			EDBM dbm = GetDBConnect();
			if (dbm == null) {
				dbm = CreateDBConnect();
				SetDBConnect(dbm);
				UtilLog.t(getClass(), "DATABASE CONNECTED !!!");
			}
			if (DBConnect(dbm)) {
				return dbm;
			}
			UtilLog.e(getClass(), "DATABASE NOT CONNECTED !!!");
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return null;
	}

	/**
	 * Map에서 사용자 기본 정보 획득
	 * - 기존 다양하게 정의된 필드명이 많기 때문에 아래와 같이 처리, DN이 없는 경우는 질의문 처리를 위해서 X 처리
	 * @param inputData DataDB2
	 * @param map MAP
	 */
	private void setUserData(DataDB2 inputData, Map<String, String> map) {

		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_USER_ID))) {
			inputData.SetData(CommonConstants.VAR_USER_ID, 	map.get(CommonConstants.VAR_USER_ID));
			inputData.SetData(CommonConstants.VAR_USER_NO, 	map.get(CommonConstants.VAR_USER_ID));
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_ID))) {
			inputData.SetData(CommonConstants.VAR_USER_ID, 	map.get(CommonConstants.VAR_ID));
			inputData.SetData(CommonConstants.VAR_USER_NO, 	map.get(CommonConstants.VAR_ID));
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_USER_TEL))) {
			inputData.SetData(CommonConstants.VAR_DEVICE, 	map.get(CommonConstants.VAR_USER_TEL));
			inputData.SetData(CommonConstants.VAR_DN, 		map.get(CommonConstants.VAR_USER_TEL));
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_DEVICE))) {
			inputData.SetData(CommonConstants.VAR_DEVICE, 	map.get(CommonConstants.VAR_DEVICE));
			inputData.SetData(CommonConstants.VAR_DN, 		map.get(CommonConstants.VAR_DEVICE));
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_DN))) {
			inputData.SetData(CommonConstants.VAR_DEVICE, 	map.get(CommonConstants.VAR_DN));
			inputData.SetData(CommonConstants.VAR_DN, 		map.get(CommonConstants.VAR_DN));
		}
		if(UtilString.isEmpty(inputData.GetData(CommonConstants.VAR_DN))) {
			inputData.SetData(CommonConstants.VAR_DN, "X");			//	내선번호 제거된 상태임(DB 질의에서 사용하기 위함)
		}
		if(UtilString.isEmpty(inputData.GetData(CommonConstants.VAR_DEVICE))) {
			inputData.SetData(CommonConstants.VAR_DEVICE, "X");		//	내선번호 제거된 상태임(DB 질의에서 사용하기 위함)
		}
	}

	/**
	 * 사용자 내선번호 갱신 (ESP_CORE.U_USER)
	 *
	 * @param map MAP
	 * @return 성공/실패
	 */
	protected boolean updateUserDN(Map<String, String>  map) {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return false;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(dbData.IsQuery(dbm, CommonConstants.FD_UPDATE_DN)) {
				DataDB2 inputData = new DataDB2();
				setUserData(inputData, map);
				UtilLog.i(getClass(),
						String.format(
								"{updateUserDN} %s=%s, %s=%s",
								CommonConstants.VAR_USER_NO,
								inputData.GetData(CommonConstants.VAR_USER_NO),
								CommonConstants.VAR_DN,
								inputData.GetData(CommonConstants.VAR_DN)
						)
				);
				return dbData.Update(dbm, CommonConstants.FD_UPDATE_DN, inputData);
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{updateUserDN} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.t(getClass(), "SetTransaction{updateUserDN} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
		return false;
	}		
	
	/**
	 *
	 * 내선번호 중복 체크 후, 메신저 알림 통보 처리
	 * 	질의 내용 : 내선 중복 체크, TITLE(내선중복 알림), CONTENT(내선중복이 발생하였습니다)
	 * 			   전송자 사번, 수신자 사번 리스트, 중복자 정보를 얻음
	 * 	중복자 정보 예시 : 내선번호 사번 이름 직위 부서
	 * 		8412 SI24028 홍길동 부장 Tech 플랫폼부
	 * 		8412 SI24029 김철수 대리 Tech 플랫폼부
	 *
	 * @param dnList 중복 내선번호 리스트
	 */
	protected void duplicateDn(List<String> dnList) {
		EDBM dbm = DBConnect();
	    if (dbm == null) {
	        return;
	    }
	    try {
	    	// IN절에서 사용하기 위한 처리, 각 요소를 '값' 형태로 감싸고 ", " 로 이어붙이기
	        String finalDnList = dnList.stream()
	            .filter(Objects::nonNull)                     						// null 제외 (필요시)
	            .filter(s -> !s.isEmpty())                    				// 빈 문자열 제외 (필요시)
	            .map(s -> "'" + s.replace("'", "''") + "'")   // SQL 주입 방지용 간단 치환
	            .collect(Collectors.joining(", "));
	        if (finalDnList.isEmpty()) {
	            return;
	        }
			DataAlert alert = new DataAlert();
	        if (dbData.IsQuery(dbm, CommonConstants.FD_DUPLICATE_DN)) {
	        	String sql = dbData.GetQuery(dbm, CommonConstants.FD_DUPLICATE_DN);
	        	String query = String.format(sql, finalDnList);
	            List<Object> resultList = dbData.Select(dbm, CommonConstants.FD_DUPLICATE_DN, query);
	            for (Object obj : resultList) {            	
	            	DataDB2 data   = (DataDB2) obj;
	            	String type    = data.GetData(CommonConstants.VAR_TYPE);					//	DUPUSER, SENDER, RECEIVERdd
	            	String title   = data.GetData(CommonConstants.VAR_TITLE);
	            	String content = data.GetData(CommonConstants.VAR_CONTENT);
	            	if(CommonConstants.VAR_SENDER.equals(type)) {								//	전송하는 사람
	            		alert.getSenderList().add(data.GetData(CommonConstants.VAR_USER_NO));
	            	}
	            	if(CommonConstants.VAR_RECEIVER.equals(type)) {								//	수신하는 사람
	            		alert.getReceiverList().add(data.GetData(CommonConstants.VAR_USER_NO));
	            	}
	            	if(CommonConstants.VAR_DUPUSER.equals(type)) {								//	중복 사용자 정보
	            		String userDN    = data.GetData(CommonConstants.VAR_DN);
		            	String userNM    = data.GetData(CommonConstants.VAR_USER_NM);
		            	String gradeNM   = data.GetData(CommonConstants.VAR_GRADE_NM);
		            	if(UtilString.isEmpty(gradeNM)) { gradeNM = "";	}
		            	String userNO    = data.GetData(CommonConstants.VAR_USER_NO);
		            	String deptNM    = data.GetData(CommonConstants.VAR_DEPT_NM);
		            	if(UtilString.isEmpty(deptNM)) { deptNM = "";	}
		     			String strData   = String.format("%-8s %-8s %s(%s) %s", userDN, userNM, gradeNM, userNO, deptNM);
	            		alert.getContentList().add(strData);
	            	}
	            	alert.setTitle(title);		//	TITLE
	            	alert.setContent(content);	//	CONTENT
	            }
	            if(!alert.getContentList().isEmpty() && !alert.getReceiverList().isEmpty()) {
	            	EThread.postMessage(Define.TID_HRREST_WORKER, alert);
	            }
	        }
	    } catch (Exception e) {
	        UtilLog.e(getClass(), "{duplicateDn} " + e.getMessage());
	    }
	}
	
	/**
	 * Map에서 DN 정보 얻기
	 * @param map MAP
	 * @return 수집된 내선번호
	 */
	protected String getDn(Map<String, String> map) {
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_USER_TEL) )) {	//	파일 실시간 배치에 정의 (USER_TEL)
			return map.get(CommonConstants.VAR_USER_TEL);
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_DEVICE))) {		//	기본은 DEVICE
			return map.get(CommonConstants.VAR_DEVICE);
		}
		if(!UtilString.isEmpty( map.get(CommonConstants.VAR_DN))) {
			return map.get(CommonConstants.VAR_DN);
		}
		return "";
	}
	
	/**
	 * 사용자 정보 갱신 단건 처리
	 *
	 * @param map MAP
	 * @return 성공/실패
	 */
	protected boolean updateUser(Map<String, String> map) {
		//	TODO 단건도 내선 중복 체크가 필요할까? 한번 더 생각해보자.
		//	TODO 일단 넣었으니.. 테스트 필요함. 시간 없어서 테스트는 하지 않음.
		boolean isResult = false;

	    // 단일 Map 처리 시에도 List 버전 재사용
		isResult = updateUser(Collections.singletonList(map));
		if(isResult) {
			//	성공이력기록
			insertHistory(map);

			List<String> dnList = new ArrayList<>();
			dnList.add(getDn(map));
            //	내선중복체크
            if(!UtilString.isEmpty(Conf.getInstance().ALERT_NOTIFY_URL())) {
                UtilLog.i(getClass(), "########################################################################");
                UtilLog.i(getClass(), "########################################################################");
                duplicateDn(dnList);
            }
		}
		return isResult;
	}

	/**
	 * 사용자 정보 갱신 다건 처리
	 * @param list List
	 * @return 성공/실패
	 */
	protected boolean updateUser(List<?> list) {
		boolean isResult = false;
		List<String> dnList = new ArrayList<>();
	    for (Object item : list) {
	        if (!(item instanceof Map)) {
	            continue;
	        }
	        @SuppressWarnings("unchecked")
	        Map<String, String> map = (Map<String, String>) item;
	        isResult = updateUserDN(map);	//	실시간 사용자 내선번호 갱신
	        dnList.add(getDn(map));
	       
	        if(isResult) {
	        	//	성공이력기록
				insertHistory(map);
	        	
		    	// 2. 원격지 내선 동기화 시도
		    	if(!UtilString.isEmpty(Conf.getInstance().USER_UPDATE_URL())) {
		    		EThread.postMessage(Define.TID_HRREST_WORKER, new DataUser(map));
		    	}
		    	// 3. CLABEL에 데이터 전달 시도
		    	if(!UtilString.isEmpty(Conf.getInstance().PHONE_DO_URL())) {
		    		EThread.postMessage(Define.TID_HRREST_WORKER, new DataPhoneDo(map));
		    	}
	        }
	    }
	    if (!dnList.isEmpty()) {          	//	내선중복체크
	    	if(!UtilString.isEmpty(Conf.getInstance().ALERT_NOTIFY_URL())) {
	    		UtilLog.i(getClass(), "########################################################################");
	    		UtilLog.i(getClass(), "########################################################################");
	    		duplicateDn(dnList);
	    	}
	    }
	    return isResult;
	}

	/**
	 * insertHistory
	 * @param map MAP
	 */
	protected void insertHistory(Map<String, String> map) {
		//	TODO 이력 쌓아야하나.. 고민 중.. 현재 개발되어 있지 않고 필드도 정의되어 있지 않음
		UtilLog.i(getClass(), "###########################################################");
		UtilLog.i(getClass(), "###########################################################");
		UtilLog.i(getClass(), "# TODO 이전 번호 => 변경 후 번호 내선번호 변경 이력 !!!! 수정 개발 필요 !!!!#");
		UtilLog.i(getClass(), "###########################################################");
		UtilLog.i(getClass(), "###########################################################");

		EDBM dbm = DBConnect();
		if (dbm == null) {
			return;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(dbData.IsQuery(dbm,  CommonConstants.FD_INSERT_HISTORY)) {
				DataDB2 inputData = new DataDB2();
				setUserData(inputData, map);
				UtilLog.i(getClass(), String.format("{insertHistory} %s=%s, %s=%s",
						CommonConstants.VAR_USER_NO,
						inputData.GetData("USER_NO"),
						CommonConstants.VAR_DN,
						inputData.GetData("DN")));
				dbData.Insert(dbm, CommonConstants.FD_INSERT_HISTORY, inputData);
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{insertHistory} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.t(getClass(), "SetTransaction{insertHistory} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
	}

	/**
	 * 사용자 정보 갱신 단건 처리
	 * @param msg RestMessage
	 */
	protected void updateUser(RESTMessage msg) {
		ECSMessageRest resp = new ECSMessageRest(msg.getRESTRespMessageID());
		resp.AddHeaderMessageParams(msg);
		Map<String, String> params = msg.GetParamsMap();
		if(msg.getMethod() == HttpMethod.POST) {
			if(updateUser(params)) {
				resp.ResponseRest(RESTMessage.RESULT_SUCCESS, RESTMessage.CAUSE_NONE);
			} else {
				resp.ResponseRest(RESTMessage.RESULT_FAIL, RESTMessage.CAUSE_UNKNOWN);
			}
		} else {
			resp.ResponseRest(RESTMessage.RESULT_FAIL, RESTMessage.CAUSE_METHOD_NOT_ALLOWED);
		}
		EThread.postMessage(Define.TID_HTTP_SERVER, resp);
	}

	/********************************************************************************************
	 *
	 ********************************************************************************************/
	@Override
	protected void onRecvMsg(Object obj) {
		if (obj instanceof RESTMessage msg) {
            if (msg.getMessageID() == RESTMessage.REQ_REST_USER) {
                updateUser(msg);                        //	단건 사용자 변경
            }
	    }
		if (obj instanceof List<?> list) {				//	다건 사용자 변경 : 실시간 파일 동기화
            updateUser(list);
		}
	}    
	protected void onExit() {}
	protected void onInit() {}
	protected void onTimer(int arg0) {}
}