package com.ecs.esp.u.hr.db;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.ecs.base.comm.UtilCom;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.proc.EThreadWorkerPoolDB2;
import com.ecs.esp.u.com.bulk.Bulk;
import com.ecs.esp.u.com.bulk.BulkResult;
import com.ecs.esp.u.hr.db.comps.DataSource;
import com.ecs.esp.u.hr.db.data.ListWrapper;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.ConfCommon;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.msg.rest.custom.RESTMessage;

/******************************************************
 * Remote DB 처리
 * 원격지 DB에 접속하여 인사 정보를 가져오는 작업 처리
 * 
 * 
 * 
 ******************************************************/
public class RDBWorker extends EThreadWorkerPoolDB2 {
	/****************************************************************
	 * Data
	 ****************************************************************/
	private final String queryFile;
	private DataSource  dbData;

	/****************************************************************
	 * Constructor
	 ****************************************************************/
	public RDBWorker(int threadID, String queryFile) {
		super(threadID);
		this.queryFile = queryFile;
	}
	
	/********************************************************************
	 * Start
	 ********************************************************************/
	public boolean Start() {
		try {
			EDBM dbm = DBConnect();
			if (dbm != null) {
				dbData = new DataSource(this.queryFile);
				dbData.Init(dbm);
				UtilLog.i(getClass(), "[RDBWorker] " + dbm.GetConnString());
			}
			return super.Start(1);	//	Conf.getInstance().DB_WORKER_ETHREADS());
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return false;
	}

	/********************************************************************
	 * Stop
	 ********************************************************************/
	public boolean Stop() {
		super.stop();
		super.DBClose();
		return true;
	}

	/********************************************************************
	 * DBConnect
	 ********************************************************************/
	private EDBM DBConnect() {
		try {
			super.Config(this.queryFile, ConfCommon.getInstance());
			EDBM dbm = GetDBConnect();
			if (dbm == null) {
				UtilLog.t(getClass(), "DATABASE TRY CONNECTED !!!");
				dbm = CreateDBConnect();
				SetDBConnect(dbm);
				UtilLog.t(getClass(), "DATABASE CONNECTED !!!");
			}
			if (DBConnect(dbm)) {
				return dbm;
			}
			UtilLog.e(getClass(), "DATABASE NOT CONNECTED !!!");
		} catch (Exception ignored) {
		}
		return null;
	}

	/**
	 * 로그
	 * @param field FD 필드
	 * @param aliases 테이블 필드
	 * @param record DataDB2
	 */
	private void logRecord(String field, List<String> aliases, DataDB2 record) {
	    Map<String, String> dataMap = record.GetMap();
	    StringBuilder sb = new StringBuilder(field)
	        .append(") ");
	    for (String alias : aliases) {
	        sb.append(alias)
	          .append("=")
	          .append(dataMap.get(alias.toLowerCase()))
	          .append(", ");
	    }
	    String line = UtilCom.replaceLast(sb.toString(), ", ", "");
	    UtilLog.i(getClass(), line);
	}

	/**
	 * LDWorker로 전송 처리
	 * @param wrapped  ListWrapper 
	 * @throws InterruptedException 예외 처리
	 */
	private void postIfConfigured(ListWrapper<Object> wrapped) throws InterruptedException {
	    String queryFile = Conf.getInstance().LDB_QUERY_FILE();
	    if (!UtilString.isEmpty(queryFile)) {
	    	EThread.postMessage(Define.TID_LDB_WORKER, wrapped);
	        // 잠깐 대기 (필요하다면 상수로 분리)
	        Thread.sleep(1000);
	    }
	}

	/**
	 * 원격지 정보 가져오기 시작
	 * @param msg RESTMessage
	 */
	protected void getRemoteDB(RESTMessage msg) {
		UtilLog.i(getClass(), "");
		UtilLog.i(getClass(), "################################################### ");
		UtilLog.i(getClass(), "################### 인사동기화 시작 ################### ");
		UtilLog.i(getClass(), "################################################### ");

		String loginID 		= msg.GetParamJson(RESTMessage.CODE_LOGIN_ID);
		String alarmID 		= msg.GetParamJson(RESTMessage.CODE_ALARM_ID);
		String token   		= msg.GetParamJson(RESTMessage.CODE_TOKEN);
		String bulkCause	= null;
		UtilLog.i(getClass(), "TOKEN["+token+"] LOGIN_ID["+loginID+"] ALARM_ID["+alarmID+"]");
		
		try {
			List<String> fieldList = new ArrayList<String>();
			EDBM dbm = DBConnect();
			if (dbm != null) {
				 msg.SetParamJson(RESTMessage.CODE_METHOD, Define.MULTIPLE_QUERY);			//	다수	질의문
				//	사이트 다수 질의를 통한 처리(MULTIPLE_QUERY)
				if(dbData.IsQuery(dbm, String.format("%s_USER", Define.PREFIX_FIELD) )) {
					fieldList.add(String.format("%s_USER", Define.PREFIX_FIELD));
				}
				if(dbData.IsQuery(dbm, String.format("%s_DEPT", Define.PREFIX_FIELD))) {
					fieldList.add(String.format("%s_DEPT", Define.PREFIX_FIELD));
				}
				if(dbData.IsQuery(dbm, String.format("%s_GRADE", Define.PREFIX_FIELD))) {
					fieldList.add(String.format("%s_GRADE", Define.PREFIX_FIELD));
				}
				if(dbData.IsQuery(dbm, String.format("%s_POSITION", Define.PREFIX_FIELD))) {
					fieldList.add(String.format("%s_POSITION", Define.PREFIX_FIELD));
				}
				
				//	사이트 질의 한방으로 처리(SINGLE_QUERY)
				if(fieldList.size() <= 0) {
					String siteQuery = String.format("%s_%s", Define.PREFIX_FIELD, Conf.getInstance().SITE());	//	GET_SOURCE_[사이트]
					if(dbData.IsQuery(dbm, siteQuery)) {
						fieldList.add(siteQuery);
						 msg.SetParamJson(RESTMessage.CODE_METHOD, Define.SINGLE_QUERY);	//	단수 질의문
					}
				}			
				int totalFields = fieldList.size();
				int processed = 0;
				UtilLog.i(getClass(), "FIELD[" + totalFields + "] FIELD LIST => " + fieldList.toString());
				
			    for (String field : fieldList) {
			        if (!dbData.IsQuery(dbm, field)) {
			            continue;
			        }
			        List<String> aliases = dbData.GetQueryAsList(dbm, field);
					List<Object> records = dbData.Select(dbm, field, new DataDB2());
				    if (!records.isEmpty()) {				    	
				    	// 사용자 수 체크 후, 에러 파악(가끔씩 원격지 DB의 자료가 미존재하는 경우 발생함)
				    	if( field.contains("USER") ||  field.contains(Conf.getInstance().SITE() )) {
					    	if (Conf.getInstance().MIN_USERS() > 0 && records.size() <= Conf.getInstance().MIN_USERS()) {
					    		UtilLog.e(getClass(), "##########################################################");
								UtilLog.e(getClass(), "# 최소 사용자수는 " + Conf.getInstance().MIN_USERS() + " 이며 현재 사용자수는 " + records.size() + " 입니다.");
					    		UtilLog.e(getClass(), "##########################################################");
								bulkCause = BulkResult.BULK_NOT_ENOUGH_USERS;
								break;
							}
				    	} 				    	
			            // 1) 로그 출력
			            records.forEach(obj -> logRecord(field, aliases, (DataDB2) obj));

			            // 2) 마지막 메시지 설정 : 수신측(LDBWorker)에서 마지막 필드를 이용해서, 동기화를 시작함
			            msg.SetParamJson(RESTMessage.CODE_FIELD, field);
			            boolean isLast = (processed + 1) >= totalFields;
			            msg.SetParamJson(RESTMessage.CODE_FINAL, String.valueOf(isLast));

			            // 3) 결과 래핑 & 전송
			            ListWrapper<Object> wrapped = new ListWrapper<>(records, msg);
			            postIfConfigured(wrapped);
			        }
			        processed++;
				}	//	end for
			}
			if(bulkCause!=null) {
				Bulk.send(Conf.getInstance().BULK_RESPONSE_URL(), Define.TID_LDB_WORKER, false, bulkCause, msg);
			}
		} catch(Exception e) {	UtilLog.e(getClass(), "{getRemoteDB} " + e); }
	}

	/**
	 * 원격지 정보 가져오기
	 * @param msg RestMessage
	 */
	protected void syncDB(RESTMessage msg) {
		try {
			getRemoteDB(msg);
		} catch(Exception e) { 
			UtilLog.e(getClass(), "{syncDB} " + e);
		}
	}	
	@Override
	protected void onRecvMsg(Object obj) {
		if(obj instanceof RESTMessage msg) {
            syncDB(msg);
		}
	}
	protected void onTimer(int timerID) {}
	protected void onExit() {}
	protected void onInit() {}
}