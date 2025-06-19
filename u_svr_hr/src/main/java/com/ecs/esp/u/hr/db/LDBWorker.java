package com.ecs.esp.u.hr.db;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.ecs.esp.u.com.define.CommonConstants;
import org.apache.poi.ss.formula.functions.T;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.EDBPostgres;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.socket.thread.proc.EThreadWorkerPoolDB2;
import com.ecs.esp.u.com.bulk.Bulk;
import com.ecs.esp.u.com.bulk.BulkResult;
import com.ecs.esp.u.com.bulk.DataBulk;
import com.ecs.esp.u.hr.db.comps.DBWork;
import com.ecs.esp.u.hr.db.comps.DataSource;
import com.ecs.esp.u.hr.db.comps.FileWork;
import com.ecs.esp.u.hr.db.data.DataTable;
import com.ecs.esp.u.hr.db.data.ListWrapper;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.ConfCommon;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.msg.rest.custom.RESTMessage;

/******************************************************
 * Local DB 처리
 * 원격지 데이터 또는 파일 정보를 읽어서 Local DB에 기입 후,
 * 이를 인사테이블에 동기화하는 처리 수행
 * 
 *
 ******************************************************/
public class LDBWorker extends EThreadWorkerPoolDB2 {
	/****************************************************************
	 * Data
	 ****************************************************************/
	private final String 		queryFile;
	private DataSource 	dbData;
	private DBWork    	dbWork;
	private FileWork    fileWork;

	/****************************************************************
	 * Constructor
	 ****************************************************************/
	public LDBWorker(int threadID, String queryFile) {
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
				UtilLog.i(getClass(), "[LDBWorker] " + dbm.GetConnString());
			}
			dbWork   = new DBWork(dbData);
			fileWork = new FileWork(dbData);
			return super.Start(Conf.getInstance().DB_WORKER_ETHREADS());
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
	
	/**********************************************************************************************************
	 * CreateTable : 기본 테이블 자동 생성
	 **********************************************************************************************************/
	protected boolean createTable(EDBM dbm, String tableName, List<String> cellList) {
		try {
			String createHead = "";
			String createBody = "";
			String createTime = "";
			String createEnd  = "";
			if(dbm instanceof EDBPostgres) {
				createHead = "CREATE TABLE %s ( ";
				createBody = "%s VARCHAR, ";
				createTime = "UPDATETIME VARCHAR";	
				createEnd  = ")";		
			} else {
				createHead = "CREATE TABLE %s ( ";
				createBody = "%s VARCHAR(512), ";
				createTime = "UPDATETIME VARCHAR(64) ";
				createEnd  = ")";		
			}
			StringBuilder body = new StringBuilder();
			for ( String data : cellList) {
				body.append(String.format(createBody, data));
				if(UtilString.isEmpty(data)) {
					return false;
				}
			}
			String sql = String.format(createHead, tableName);
			sql += body;	//	UtilCom.replaceLast(body, ",", "");
			sql += createTime;
			sql += createEnd;
			return dbData.CreateTable(dbm, null, tableName, sql);
		//	return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	
	protected void dbBulk(DataBulk data) throws Exception {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return;
		}
		try {
			String sql = null;
			dbData.SetTransaction(dbm, true);
			if (data.isDailyBatch()) {
				if (dbData.IsQuery(dbm, CommonConstants.FD_INSERT_BATCH)) {
					UtilLog.i(getClass(), " 일배치(BATCH), 항목=" + data.title);
					dbData.Update(dbm, CommonConstants.FD_INSERT_BATCH, data);
				} else {
					if(dbm instanceof EDBPostgres) {
						data.cmplt_dt = UtilCalendar.getLongToString("yyyyMMddHHmmssSSS");
						sql = String.format(Bulk.SQLBatchv2, data.req_url, "SYSTEM", Define.COMPLETED, data.title, data.description, data.result, data.cause);
					}
					if(!UtilString.isEmpty(sql)) {
						UtilLog.i(getClass(), " 일배치(SQLBatchv2), 항목=" + data.title);
						dbData.Execute(dbm, sql);
					} else {
						UtilLog.e(getClass(), " 일배치, 항목=" + data.title + " 질의 정보가 없습니다.");
					}
				}
			} else {
				if (dbData.IsQuery(dbm, CommonConstants.FD_UPDATE_BULK)) {
					UtilLog.i(getClass(), " 실시간(BULK), 항목=" + data.title);
					dbData.Update(dbm, CommonConstants.FD_UPDATE_BULK, data);
				} else {
					if(dbm instanceof EDBPostgres) {
						data.cmplt_dt = UtilCalendar.getLongToString("yyyyMMddHHmmssSSS");
						sql = String.format(Bulk.SQLBulkv2, data.title, data.description, data.file_nm, data.result, data.cause, data.token);
					}
					if(!UtilString.isEmpty(sql)) {
						UtilLog.i(getClass(), " 실시간(SQLBulkv2), 항목=" + data.title);
						dbData.Execute(dbm, sql);
					} else {
						UtilLog.e(getClass(), " 실시간, 항목=" + data.title + " 질의 정보가 없습니다.");
					}
				}
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{dbBulk} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.i(getClass(), "SetTransaction{dbBulk} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
	}

	/******************************************************************************
	 * updateDBSiteAndTenantAndDept : 사이트/Tenant/부서 동기화
	 * - 부서 질의 결과에 사이트와 Tenant 정보가 존재하여야함.
	 ******************************************************************************/
	protected BulkResult updateDBSiteAndTenantAndDept(String deptField, String method) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause("updateDBSiteAndTenantAndDept", BulkResult.BULK_DB_CONNECTION_FAIL);
			return result;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(!dbData.IsQuery(dbm, deptField)) {
				UtilLog.i(getClass(), "[ " + deptField + " ] 필드 미정의 {"+method+"} \n");
				result.setCause("{updateDBSiteAndTenantAndDept} [ " + deptField + " ] 필드 미정의 {"+method+"}");
				return result;
			} else {
				UtilLog.i(getClass(), "[ " + deptField + " ] 사이트,테넌트,부서 분석중 {"+method+"} \n");
			} 
			List<Object> deptInputList = dbData.Select(dbm, deptField, new DataDB2());	//	현재 DB의 부서 정보를 얻음
			
			// SITE 처리
			dbWork.dbSite(dbm, deptInputList);
			
			// TENANT 처리
			dbWork.dbTenant(dbm, deptInputList);
			
			// 과 처리(NTS)
			dbWork.dbDivision(dbm, deptInputList);	//	DISABLE_DIVISION, MERGE_DIVISION 미정의시 사용하지 않음
			
			// 팀 처리(NTS)
			dbWork.dbTeam(dbm, deptInputList);		//	DISABLE_TEAM, MERGE_TEAM 미정의시 사용하지 않음
						
			// DEPT 처리
			int deptCnt = dbWork.dbDept(dbm, deptInputList, method);	
			if (deptCnt < 0) { // 부서 음수일 경우 에러임.
				result.setCause("updateDBSiteAndTenantAndDept", BulkResult.BULK_DB_QUERY_FAIL);
				return result;
			}
			result.setResult(true);
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{updateDBSiteAndTenantAndDept("+deptField+")} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.i(getClass(), "SetTransaction{updateDBSiteAndTenantAndDept("+deptField+")} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
		result.setCause("updateDBSiteAndTenantAndDept", BulkResult.BULK_UNKNOWN);
		return result;
	}
	
	/****************************************************************************************
	 * 직위 처리
	 ****************************************************************************************/
	protected BulkResult updateDBGrade(String gradeField) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause("{updateDBGrade} DataBase 연결 실패");
			return result;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(!dbData.IsQuery(dbm, gradeField)) {
				UtilLog.i(getClass(), "[ " + gradeField + " ] 필드 미정의 \n");
				result.setResult(true);		//	미정의하는 경우가 존재함.
				result.setCause("{updateDBGrade} [ " + gradeField + " ] 필드 미정의 ");
				return result;
			} else {
				UtilLog.i(getClass(), "[ " + gradeField + " ] 필드 분석중 \n");
			} 
			List<Object> gradeInputList = dbData.Select(dbm, gradeField, new DataDB2());

			if(dbWork.dbGrade(dbm, gradeInputList) != null){
				UtilLog.t(getClass(), "updateDBGrade} dbGrade");
			}
			result.setResult(true);			//	성공
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{updateDBGrade("+gradeField+")} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			if(dbData.IsQuery(dbm, gradeField)) {
				UtilLog.i(getClass(), "SetTransaction{updateDBGrade("+gradeField+")} 처리 완료");
			}
			dbData.SetTransaction(dbm, false);
		}
		result.setCause("{updateDBGrade} 잘알려지지 않은 에러");
		return result;
	}
	
	/****************************************************************************************
	 * updateDBPosition : 직책 처리
	 ****************************************************************************************/
	protected BulkResult updateDBPosition(String positionField) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause("{updateDBPosition} DataBase 연결 실패");
			return result;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(!dbData.IsQuery(dbm, positionField)) {
				UtilLog.i(getClass(), "[ " + positionField + " ] 필드 미정의 \n");
				result.setResult(true);		//	미정의하는 경우가 존재함.
				result.setCause("{updateDBPosition} [ " + positionField + " ] 필드 미정의 ");
				return result;
			} else {
				UtilLog.i(getClass(), "[ " + positionField + " ] 필드 분석중 \n");
			} 
			List<Object> positionInputList = dbData.Select(dbm, positionField, new DataDB2());

			if(dbWork.dbPosition(dbm, positionInputList) != null){
				UtilLog.t(getClass(), "{updateDBPosition} dbPosition.");
			}
			result.setResult(true);			//	성공
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{updateDBPosition("+positionField+")} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			if(dbData.IsQuery(dbm, positionField)) {
				UtilLog.i(getClass(), "SetTransaction{updateDBPosition("+positionField+")} 처리 완료");
			}
			dbData.SetTransaction(dbm, false);
		}
		result.setCause("{updateDBPosition} 잘알려지지 않은 에러");
		return result;
	}
	
	/****************************************************************************************
	 * 사용자/Member 테이블 처리
	 ****************************************************************************************/
	protected BulkResult updateDBUserAndMember(String userField) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause("{updateDBUserAndMember} DataBase 연결 실패");
			return result;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(!dbData.IsQuery(dbm, userField)) {
				UtilLog.e(getClass(), "[ " + userField + " ] 필드 미정의 \n");
				result.setCause("{updateDBUserAndMember} [ " + userField + " ] 필드 미정의 ");		//	반드시 필요한 질의
				return result;
			} else {
				UtilLog.i(getClass(), "[ " + userField + " ] 필드 분석중 \n");
			} 
			List<Object> userInputList = dbData.Select(dbm, userField, new DataDB2());
			if(userInputList.isEmpty()) {
				result.setCause("{updateDBUserAndMember} 질의 결과가 존재하지 않습니다.");
				return result;
			}
			//	사용자 처리(ESP_CORE.U_USER)
			dbWork.dbUser(dbm, userInputList);

			//	사용자 서비스 처리(ESP_UC.U_USER_SERVICE)
			dbWork.dbUserService(dbm, userInputList);
			
			if(Conf.getInstance().CREATE_MEMBER_ENABLE()) {
				// Member 처리(ESP_CORE.U_MEMBER)
				dbWork.dbMember(dbm, userInputList);
			
				// User 테이블의 정보를 이용해서 Member State를 DEL로 변경(ESP_CORE.U_MEMBER)
				dbWork.delMemberState(dbm);
				
				// Member 정보를 이용해서 User 테이블의 LOGIN_FL 설정값 변경
				dbWork.updateUserLoginFL(dbm);
			}
			result.setResult(true);			//	성공
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{updateDBUserAndMember("+userField+")} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.i(getClass(), "SetTransaction{updateDBUserAndMember("+userField+")} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
		result.setCause("{updateDBUserAndMember} 잘알려지지 않은 에러");
		return result;
	}	
	
	/**********************************************************************************************************
	 * endQuery
	 **********************************************************************************************************/
	protected void endQuery() {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return;
		}
		try {
			dbData.SetTransaction(dbm, true);
			for(int i = 0; i < 12; i++) {
				String query = String.format("QUERY%03d",  i);
				if(!dbData.IsQuery(dbm, query)) {
					break;
				}
				dbData.Execute(dbm, dbData.GetQuery(dbm, query));
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{endQuery} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.t(getClass(), "SetTransaction{endQuery} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
	}
	
	/****************************************************************************************
	 * Redis 동기화
	 ****************************************************************************************/
	public void Redis(String strUrl, String apiKey) {
	    try {
		//	String urlString = strUrl;
            URL url = new URL(strUrl);

            // HttpURLConnection 객체 생성 및 설정
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("apiKey", apiKey); 	// 헤더에 key 값 추가
            connection.setConnectTimeout(3000); 				// 연결 타임아웃 3초
            connection.setReadTimeout(3000); 					// 읽기 타임아웃 3초

            // 응답 코드 확인
            int responseCode = connection.getResponseCode();
            UtilLog.i(getClass(), "[Redis] Response Code: " + responseCode);

            // 응답 내용 읽기
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder response = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            in.close();
            UtilLog.i(getClass(), "[Redis] Response: " + response.toString());
		} catch (Exception e) {
            UtilLog.e(getClass(), "[Redis] "+ e);
        }
	}

	protected void logBeautiful(String data) {
		UtilLog.i(getClass(), "########################################################################################");
		UtilLog.i(getClass(), "##																						");
		UtilLog.i(getClass(), String.format("##				%s																", data));
		UtilLog.i(getClass(), "##																						");
		UtilLog.i(getClass(), "########################################################################################");
		try {
			Thread.sleep(2000);
		} catch (Exception ignored) {
		}
	}
	
	/*************************************************************************************
	 * syncDB : 사용자 정보 동기화
	 *************************************************************************************/
	@SuppressWarnings("unchecked")
	protected  BulkResult syncDBFile(List<?> obj, RESTMessage msg) {
		BulkResult result = null;
		if (!Conf.getInstance().LOCAL_HR_ENABLE()) {
			UtilLog.i(getClass(), " HR 처리만 수행함 { LOCAL_HR_ENABLE=" + Conf.getInstance().LOCAL_HR_ENABLE() + " } ");
			return null;
		}
		if (!obj.isEmpty() && obj.get(0) instanceof DataTable) {	
			String method = Define.MULTIPLE_QUERY;
			logBeautiful("FILE 분석 처리 시작 { " + method + "}");

			List<DataTable> list = (List<DataTable>) obj;
			for(DataTable data : list) {
				UtilLog.t(getClass(), "FILE="+data.getName() +
						", TABLE=" +data.getTable() +
						", QUERY_FILE="+data.getQueryField() +
						", FULLPATH=" + data.getFullPath());
			}
			
			for(DataTable data : list) {
				if(!UtilString.isEmpty(data.getQueryField()) && data.getQueryField().contains("DEPT")) {
					result = updateDBSiteAndTenantAndDept( String.format("%s_HR_DEPT", Define.PREFIX_FIELD), method);	
				}
			}
			for(DataTable data : list) {
				if(!UtilString.isEmpty(data.getQueryField()) && Objects.requireNonNull(result).isResult() && data.getQueryField().contains("GRADE")) {
					result = updateDBGrade( String.format("%s_HR_GRADE", Define.PREFIX_FIELD) );
				}
			}
			for(DataTable data : list) {
				if(!UtilString.isEmpty(data.getQueryField()) && Objects.requireNonNull(result).isResult()  && data.getQueryField().contains("POSITION")) {
					result = updateDBPosition( String.format("%s_HR_POSITION", Define.PREFIX_FIELD) );
				}
			}
			for(DataTable data : list) {
				if(!UtilString.isEmpty(data.getQueryField()) && Objects.requireNonNull(result).isResult() && data.getQueryField().contains("USER")) {
					result = updateDBUserAndMember( String.format("%s_HR_USER", Define.PREFIX_FIELD) );
				}
			}
			if(Objects.requireNonNull(result).isResult()) {
				endQuery();
				for(int i = 0; i < 12; i++) {
					if(!UtilString.isEmpty(Conf.getInstance().REDIS_URL(i))) {
						Redis(Conf.getInstance().REDIS_URL(i) , Conf.getInstance().REDIS_APIKEY());
					}
				}
			}
			logBeautiful("FILE 분석 처리 완료 { " + method + " }");
		}
		return result;
	}

	/***********************************************************************************
	 * insertFile : 파일을 읽어서 테이블을 생성하고 데이터를 insert 시킴
	 ***********************************************************************************/
	protected BulkResult insertFile(List<DataTable> list) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause("{insertFile} 데이터베이스 연결 실패");
			return result;
		}
		try {
			int valListSize = 0;
			dbData.SetTransaction(dbm, true);
			for (DataTable data : list) {
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "TABLE=" + data.getTable() +
						", QUERY_FIELD=" + data.getQueryField() +
						", FULLPATH=" + data.getFullPath() +
						", FILENAME=" + data.getName());

				int insertSize = fileWork.insertSplitFile(dbm, data.getFullPath(), data.getTable(),  Conf.getInstance().FILE_DELIMITER());
				if(insertSize < 0) {
					result.setCause("{insertFile} 데이터베이스 연결 실패");
					return result;
				}
				if( data.getQueryField().contains("USER") ) {	
					valListSize = insertSize;
				}
			}
			if (Conf.getInstance().MIN_USERS() > 0 && valListSize <= Conf.getInstance().MIN_USERS()) {
				UtilLog.e(getClass(), "##########################################################");
				UtilLog.e(getClass(), "# 최소 사용자수는 " + Conf.getInstance().MIN_USERS() + " 이며 현재 사용자수는 " + valListSize + " 입니다.");
	    		UtilLog.e(getClass(), "##########################################################");
	    		result.setCause("{insertFile} 최소 사용자수 미달");
				return result;
			}
			result.setResult(true);
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{insertFile} " + e);
			dbData.Rollback(dbm);
		} finally {
			dbData.SetTransaction(dbm, false);
		}
		result.setCause("{insertFile} 잘알려지지 않은 에러");
		return result;
	}
	@SuppressWarnings("unchecked")
	protected BulkResult processFile(List<?> obj, RESTMessage msg) {
		BulkResult result = null;
		try {
			List<DataTable> list = (List<DataTable>)obj;
			result = insertFile(list);
			if(result.isResult()) {
				return syncDBFile(list, msg);
			}
		} catch(Exception e) { UtilLog.e(getClass(), "{processFile} " + e);}
		return result;
	}

	/****************************************************************************************
	 * 임시 테이블에 데이터 등록
	 ****************************************************************************************/
	protected boolean createAndInsertDB(List<?> insertList, String field) {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return false;
		}
		String tableName = "";
		try {
			DataDB2 first = ((DataDB2)insertList.get(0));	
			List<String> cellList = new ArrayList<String>();
			for( Map.Entry<String, String> elem : first.GetMap().entrySet()) {	//	테이블을 만들 필드를 얻음.
				String key = elem.getKey();
				if(!key.startsWith("yyyymmdd") && !key.startsWith("sequence") && !key.startsWith("final") && !key.startsWith("login_id") && !key.startsWith("alarm_id") && !key.startsWith("query")) {
					cellList.add(elem.getKey().toUpperCase());
				}
			}
			dbData.SetTransaction(dbm, true);
			tableName = field.replaceAll("GET_SOURCE", "HR");		//	GET_SOURCE_USER -> HR_USER 테이블 생성
			if(dbData.createTable(dbm, Conf.getInstance().HR_SCHEMA(), tableName, cellList)) {
				if(dbData.IsQuery(dbm, String.format("DELETE_%s", tableName))) {
					dbData.Delete(dbm,  String.format("DELETE_%s", tableName), new DataDB2());
				} else {
					dbData.Execute(dbm, String.format("DELETE FROM %s", tableName));
				}
				return dbData.Insert(dbm, String.format("INSERT_%s", tableName), insertList);
			} else {
				UtilLog.e(getClass(), "TABLE 생성에 실패하였습니다.(TABLE=" + tableName +", FIELD="+ cellList.toString()+")");
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{insertDB("+tableName+")} " + e);
			dbData.Rollback(dbm);
		} finally {
			UtilLog.i(getClass(), "[insertDB::SetTransaction] 처리 완료(테이블명="+tableName+")");
			dbData.SetTransaction(dbm, false);
		}
		return false;
	}

	protected BulkResult processDB(List<?> obj, RESTMessage msg) {
		BulkResult result = null;
		try {
			String field 	 = msg.GetParamJson(RESTMessage.CODE_FIELD);	
			String method 	 = msg.GetParamJson(RESTMessage.CODE_METHOD);
	        boolean bfinal 	 = Boolean.parseBoolean(msg.GetParamJson(RESTMessage.CODE_FINAL));
			UtilLog.t(getClass(), "{processDB} FIELD="+field+", FINAL="+bfinal);
		
			if(method.equals(Define.MULTIPLE_QUERY)) {		//	질의문 다수로 처리하는 방식.
				@SuppressWarnings("unchecked")
				List<DataDB2> list = (List<DataDB2>)obj;
				if(createAndInsertDB(list, field)) {		//	테이블 자동 생성 및, Insert
					if(bfinal) {							//	마지막 처리
						if (!Conf.getInstance().LOCAL_HR_ENABLE()) {
							UtilLog.i(getClass(), " HR 처리만 수행함 { LOCAL_HR_ENABLE=" + Conf.getInstance().LOCAL_HR_ENABLE() + " } [ " + method + " ]");
							return null;
						}
						
						logBeautiful("DB 분석 처리 시작 { " + method + " }");
						result = updateDBSiteAndTenantAndDept( String.format("%s_HR_DEPT", Define.PREFIX_FIELD), method );	//	GET_SOURCE_HR_DEPT
						if(result.isResult()) {
							result = updateDBGrade( String.format("%s_HR_GRADE", Define.PREFIX_FIELD) );					//	GET_SOURCE_HR_GRADE
						}
						if(result.isResult()) {
							result = updateDBPosition( String.format("%s_HR_POSITION", Define.PREFIX_FIELD) );				//	GET_SOURCE_HR_POSITION
						}
						if(result.isResult()) {
							result = updateDBUserAndMember( String.format("%s_HR_USER", Define.PREFIX_FIELD) );				//	GET_SOURCE_HR_USER
						}
						if(result.isResult()) {
							endQuery();
							for(int i = 0; i < 12; i++) {
								if(!UtilString.isEmpty(Conf.getInstance().REDIS_URL(i))) {
									Redis(Conf.getInstance().REDIS_URL(i) , Conf.getInstance().REDIS_APIKEY());
								}
							}
						}
						logBeautiful("DB 분석 처리 완료 { " + method + " }");
					}
				} 
			} else {					//	하나의 질의문으로 처리하는 방식
				@SuppressWarnings("unchecked")
				List<DataDB2> list = (List<DataDB2>)obj;
				if(createAndInsertDB(list, field)) {
					if (!Conf.getInstance().LOCAL_HR_ENABLE()) {
						UtilLog.i(getClass(), " HR 처리만 수행함 { LOCAL_HR_ENABLE=" + Conf.getInstance().LOCAL_HR_ENABLE() + " } [ " + method + " ]");
						return null;
					}
					
					logBeautiful("DB 분석 처리 시작 { " + method + " }");
					result = updateDBSiteAndTenantAndDept( String.format("%s_HR_DEPT", Define.PREFIX_FIELD), method);			//	GET_SOURCE_HR_DEPT
					if(result.isResult()) {
						result = updateDBGrade( String.format("%s_HR_GRADE", Define.PREFIX_FIELD) );							//	GET_SOURCE_HR_GRADE
					}
					if(result.isResult()) {
						result = updateDBPosition( String.format("%s_HR_POSITION", Define.PREFIX_FIELD) );					//	GET_SOURCE_HR_POSITION
					}
					if(result.isResult()) {
						result = updateDBUserAndMember( String.format("%s_HR_USER", Define.PREFIX_FIELD) );					//	GET_SOURCE_HR_USER
					}
					if(result.isResult()) {
						endQuery();
						for(int i = 0; i < 12; i++) {
							if(!UtilString.isEmpty(Conf.getInstance().REDIS_URL(i))) {
								Redis(Conf.getInstance().REDIS_URL(i) , Conf.getInstance().REDIS_APIKEY());
							}
						}
					}
					logBeautiful("DB 분석 처리 완료 { " + method + " }");
				}
			}
		} catch(Exception e) { UtilLog.e(getClass(), "{processDB} " + e);}
		return result;
	}

	/********************************************************************************************
	 * DB2DB   : MULTIPLE(다건질의), SINGLE(단건질의) - 원격지 DB를 읽어서 Insert 시키고 배치 처리
	 * FILE2DB : MULTIPLE(다건질의) - 파일을 읽어서 Insert 시키고 배치 처리
	 * BULK,BATCH 결과 갱신
	 ********************************************************************************************/
	@Override
	protected void onRecvMsg(Object obj) {
		if (obj instanceof ListWrapper<?>) {
			@SuppressWarnings("unchecked")
			ListWrapper<T> listWrapper = (ListWrapper<T>) obj;
	        RESTMessage msg  = listWrapper.getRESTMessage();
	        List<?> rawItems = listWrapper.getItems();
	        //	DB2DB	: MULTIPLE(다건질의), SINGLE(단건질의)
	        if (!rawItems.isEmpty() && rawItems.get(0) instanceof DataDB2) {
				@SuppressWarnings("unchecked")
				List<DataDB2> list = (ArrayList<DataDB2>) rawItems;
	        	BulkResult result = processDB(list, msg);	
				if (result != null) {
					Bulk.send(Conf.getInstance().BULK_RESPONSE_URL(), Define.TID_LDB_WORKER, result.isResult(), result.getCause(), msg);
				}
	        }
	        //	FILE2DB	: MULTIPLE(다건질의)
	        if (!rawItems.isEmpty() && rawItems.get(0) instanceof DataTable) {
				@SuppressWarnings("unchecked")
				List<DataTable> list = (ArrayList<DataTable>) rawItems;
	            BulkResult result = processFile(list, msg);
	            if (result != null) {
					Bulk.send(Conf.getInstance().BULK_RESPONSE_URL(), Define.TID_LDB_WORKER, result.isResult(), result.getCause(), msg);
				}
	        }
	    }
	    //	BULK, BATCH DB 갱신 처리
	    if (obj instanceof DataBulk) {
	    	try {
	    		dbBulk((DataBulk)obj);
	    	} catch(Exception ignored) { }
	    }
	}    
	protected void onExit() {}
	protected void onInit() {}
	protected void onTimer(int arg0) {}
}