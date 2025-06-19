package com.ecs.esp.u.alert.define;

import com.ecs.base.comm.EProperties;
import com.ecs.base.comm.UtilCom;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;

import java.util.ArrayList;
import java.util.List;

public class Conf extends EProperties {
	/********************************************************************
	 * Instance
	 ********************************************************************/
	private static Conf instance;
	public static Conf getInstance() {
		if(instance == null) { instance = new Conf(); }
		return instance;
	}
	public int SYSTEM_PORT() { return PropInt("SYSTEM_PORT"); }
	public String APP_NAME() { return PropString("APP_NAME"); }
	public int SYSTEM_DELAY() { return PropInt("SYSTEM_DELAY"); }
	
	/********************************************************************
	 * Instance
	 ********************************************************************/
	// HTTP SERVER
	final private static String PROP_SERVER_PORT			=	"SERVER_PORT";
	final private static String PROP_SERVER_IP				=	"SERVER_IP";
	final private static String PROP_SERVER_HTTP_TIMEOUT	=	"SERVER_HTTP_TIMEOUT";	

	// RESPONSE URL
	final private static String PROP_BULK_RESPONSE_URL		=	"BULK_RESPONSE_URL";			//	Token 응답 URL		
	
	//	TESTER
	final private static String PROP_TESTER_PORT			=	"TESTER_PORT";
	final private static String PROP_TESTER_TIMEOUT			=	"TESTER_TIMEOUT";
	final private static String PROP_TESTER_SAMPLE_FILE		=	"TESTER_SAMPLE_FILE";
	final private static String PROP_TESTER_MAKER_FILE		=	"TESTER_MAKER_FILE";

	final private static String PROP_SITE					=	"SITE";
	final private static String PROP_SYNC_TIME				=	"SYNC_TIME";
	final private static String PROP_SYNC_TIME_INTERVAL		=	"SYNC_TIME_INTERVAL";
	final private static String PROP_QUERY_FILE				=	"QUERY_FILE";
	final private static String PROP_CREATE_TABLE			=	"CREATE_TABLE";
	final private static String PROP_DB_WORKER_ETHREADS		=	"DB_WORKER_ETHREADS";
	final private static String PROP_CREATE_TABLE_DAY		=	"CREATE_TABLE_DAY";				//	yyyyMMdd 테이블 생성 기간 정의

	public String SITE() 					{ return PropString(PROP_SITE);					}
	public String HEADER_KEY(String site, int i) {
		String data = PropString(  String.format("%s_HEADER%02d", site, i) );
		return UtilString.Split(data, ";", 0);
	}
	public String HEADER_VAL(String site, int i) {
		String data = PropString(  String.format("%s_HEADER%02d", site, i) );
		return UtilString.Split(data, ";", 1);
	}
	public String PARAM_KEY(String site, int i) {
		String data = PropString(  String.format("%s_PARAM%02d", site, i) );
		return UtilString.Split(data, ";", 0);
	}
	public String PARAM_VAL(String site, int i) {
		String data = PropString(  String.format("%s_PARAM%02d", site, i) );
		return UtilString.Split(data, ";", 1);
	}
	public String URL(String site) {
		return PropString(  String.format("%s_URL", site) );
	}
	public String DATA_KEY(String site, int i) {
		String data = PropString(  String.format("%s_DATA%02d", site, i) );
		return UtilString.Split(data, ";", 0);
	}
	public String DATA_VAL(String site, int i) {
		String data = PropString(  String.format("%s_DATA%02d", site, i) );
		return UtilString.Split(data, ";", 1);
	}
	public int CONNTO(String site) {
		String data = PropString(  String.format("%s_TIMEOUT", site) );
		String timeout = UtilString.Split(data, ";", 0);
		if(UtilString.isEmpty(timeout)) {
			return 5000;
		}
		return Integer.parseInt(timeout);
	}
	public int READTO(String site) {
		String data = PropString(  String.format("%s_TIMEOUT", site) );
		String timeout = UtilString.Split(data, ";", 1);
		if(UtilString.isEmpty(timeout)) {
			return 5000;
		}
		return Integer.parseInt(timeout);
	}
	public String SYNC_TIME() 				{ return PropString(PROP_SYNC_TIME);			}
	public String QUERY_FILE() 				{ return PropString(PROP_QUERY_FILE);			}	
	public int    	START_TABLE() { 
		String data = PropString(PROP_CREATE_TABLE);
		String[] split = data.split(";");
		if(split.length>=2) {
			return Integer.parseInt(split[0]);
		}
		return 0;
	}
	public int    	END_TABLE() { 
		String data = PropString(PROP_CREATE_TABLE);
		String[] split = data.split(";");
		if(split.length>=2) {
			return Integer.parseInt(split[1]);
		}
		return 3;
	}
	public int START_TABLE_DAY() {
		String data = PropString(PROP_CREATE_TABLE_DAY);
		String[] split = data.split(";");
		if (split.length >= 2) {
			return Integer.parseInt(split[0]);
		}
		return (365 * 3) + 1;
	}
	public int END_TABLE_DAY() {
		String data = PropString(PROP_CREATE_TABLE_DAY);
		String[] split = data.split(";");
		if (split.length >= 2) {
			return Integer.parseInt(split[1]);
		}
		return (365 * 3) + 1;
	}	
	public int SYNC_TIME_INTERVAL_START() {
		try {
			String data = PropString(PROP_SYNC_TIME_INTERVAL);
			if (!UtilString.isEmpty(data)) {
				return Integer.parseInt(UtilString.Split(data, ";", 0));
			}
		} catch (Exception ignored) {
		}
		return 10;
	}
	public int SYNC_TIME_INTERVAL() {
		try {
			String data = PropString(PROP_SYNC_TIME_INTERVAL);
			if (!UtilString.isEmpty(data)) {
				return Integer.parseInt(UtilString.Split(data, ";", 1));
			}
		} catch (Exception ignored) {
		}
		return 1;
	}	
	public int DB_WORKER_ETHREADS() {
		int data = PropInt(PROP_DB_WORKER_ETHREADS);
		if (data <= 0) {
			return 1;
		}
		return data;
	}
	public int ALERT_WORKER_QSIZE(){
		int data = PropInt("ALERT_WORKER_QSIZE");
		if (data <= 0) {
			return 1000;
		}
		return data;
	}
	//	HTTP SERVER
	public int SERVER_PORT() 						{ 
		return PropInt(PROP_SERVER_PORT);					
	}
	public String SERVER_IP() 						{ 
		return PropString(PROP_SERVER_IP);				
	}
	public int SERVER_HTTP_TIMEOUT() 				{ 
		return PropInt(PROP_SERVER_HTTP_TIMEOUT);			
	}

	//	RESPONSE URL
	public String BULK_RESPONSE_URL() {
		return PropString(PROP_BULK_RESPONSE_URL);
	}
	
	//	TESTER
	public int TESTER_PORT() {
		return PropInt(PROP_TESTER_PORT);
	}	
	public int TESTER_TIMEOUT() {
		return PropInt(PROP_TESTER_TIMEOUT);
	}
	public String TESTER_MAKER_FILE() {
		return PropString(PROP_TESTER_MAKER_FILE);
	}
	public String TESTER_MAKER_PATH() {
		String data = PropString(PROP_TESTER_MAKER_FILE);
		return UtilFile.getPath(data);
	}
	public String TESTER_SAMPLE_FILE() {
		return PropString(PROP_TESTER_SAMPLE_FILE);
	}
	public String TESTER_SAMPLE_PATH() {
		String data = PropString(PROP_TESTER_SAMPLE_FILE);
		return UtilFile.getPath(data);
	}
	public String TESTER_MENU_FILENAME() {
		String data = PropString(PROP_TESTER_SAMPLE_FILE);
		return UtilFile.getFileName(data);
	}
	
	// HTTP SERVER
	final private static String PROP_SERVER_HTTP_PORT				=	"SERVER_HTTP_PORT";
	final private static String PROP_SERVER_HTTP_IP					=	"SERVER_HTTP_IP";
		
	// HTTP SERVER
	public int SERVER_HTTP_PORT() {
		return PropInt(PROP_SERVER_HTTP_PORT);
	}
	public String SERVER_HTTP_IP() {
		return PropString(PROP_SERVER_HTTP_IP);
	}

	/********************************************************************
	 * LoadLogLevel
	 ********************************************************************/
	@Override
	public boolean LoadLogLevel() {
//		try {
//		} catch (Exception e) { }
		return super.LoadLogLevel();
	}	
}