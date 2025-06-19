package com.ecs.esp.u.hr.define;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ecs.base.comm.EProperties;
import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.esp.u.hr.db.data.DataTable;

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
	
	final private static String PROP_SITE							=	"SITE";
	final private static String PROP_MIN_USERS						=	"MIN_USERS";
	final private static String PROP_SEPARATOR						=	"SEPARATOR";
	final private static String PROP_HDB_QUERY_FILE					=	"HDB_QUERY_FILE";
	final private static String PROP_RDB_QUERY_FILE					=	"RDB_QUERY_FILE";
	final private static String PROP_LDB_QUERY_FILE					=	"LDB_QUERY_FILE";
	final private static String PROP_SYNC_TIME						=	"SYNC_TIME";					//	동기화시각
	final private static String PROP_SYNC_TIME_INTERVAL				=	"SYNC_TIME_INTERVAL";			//	DB Timer
	final private static String PROP_DB_WORKER_ETHREADS				=	"DB_WORKER_ETHREADS";			//	DB 처리하는 쓰레드 개수
	final private static String PROP_CREATE_TABLE					=	"CREATE_TABLE";
	final private static String PROP_CREATE_TABLE_DAY				=	"CREATE_TABLE_DAY";				//	yyyyMMdd 테이블 생성 기간 정의
	final private static String PROP_PICTURE_PATH					=	"PICTURE_PATH";
	final private static String PROP_CRYPTO_FIELD					=	"CRYPTO_FIELD";	
	final private static String PROP_ENCRYPTION						=	"ENCRYPTION";
	final private static String PROP_REDIS_APIKEY					=	"REDIS_APIKEY";
	final private static String PROP_CREATE_MEMBER_ENABLE			=	"CREATE_MEMBER_ENABLE";
	final private static String PROP_LOCAL_HR_ENABLE				=	"LOCAL_HR_ENABLE";
	final private static String PROP_HR_SCHEMA						=	"HR_SCHEMA";

	public String HDB_QUERY_FILE()		{	return PropString(PROP_HDB_QUERY_FILE);		}
	public String RDB_QUERY_FILE()		{	return PropString(PROP_RDB_QUERY_FILE);		}
	public String LDB_QUERY_FILE()		{	return PropString(PROP_LDB_QUERY_FILE);		}
	public String SITE()				{	return PropString(PROP_SITE);				}
	public int    MIN_USERS()			{	return PropInt(PROP_MIN_USERS);				}
	public String SEPARATOR() {
		String data = PropString(PROP_SEPARATOR);
		if (UtilString.isEmpty(data)) {
			return "‡"; // 기본값 리턴
		}
		return data;
	}
	public String SITE_URL(int i, String site) {
		String data = PropString(String.format("%s_URL", site));
		if(UtilString.isEmpty(data)) {
			return null;
		}
		String[] list = data.split(";");
		if(list.length == 2) {
			return list[1];
		}
		return null;
	}
	public String SITE_FIELD(int i, String site) {
		String data = PropString(String.format("%s_URL", site));
		if(UtilString.isEmpty(data)) {
			return null;
		}
		String[] list = data.split(";");
		if(list.length == 2) {
			return list[0];
		}
		return null;
	}

	public String SYNC_TIME() {
		return PropString(PROP_SYNC_TIME);
	}
	public String HR_SCHEMA() {
		return PropString(PROP_HR_SCHEMA);
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
	public int START_TABLE() {
		String data = PropString(PROP_CREATE_TABLE);
		String[] split = data.split(";");
		if (split.length >= 2) {
			return Integer.parseInt(split[0]);
		}
		return 0;
	}
	public int END_TABLE() {
		String data = PropString(PROP_CREATE_TABLE);
		String[] split = data.split(";");
		if (split.length >= 2) {
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
	public String PICTURE_PATH() {
		return PropString(PROP_PICTURE_PATH);
	}
	public int CRYPTO() {
		try {
			String encryption = PropString(PROP_CRYPTO_FIELD);
			if(!UtilString.isEmpty(encryption)) {
				String data = UtilString.Split(encryption, ";", 0);
				if(data.equalsIgnoreCase("FileScrtySSO")) {
					return Define.FileScrtySSO;
				} else if(data.equalsIgnoreCase("EncryptDB")) {
					return Define.EncryptDB;
				}
			}
		} catch (Exception ignored) {}
		return Define.None;
	}	
	
	List<String> cryptoField = new ArrayList<>();
	public List<String> CRYPTO_FIELD() {
		try {
			cryptoField.clear();
			String data = UtilString.Split(PropString(PROP_CRYPTO_FIELD), ";", 1);
			String[] list = data.split(",");
            Collections.addAll(cryptoField, list);
			return cryptoField;
		} catch (Exception ignored) {
		}
		return null;
	}	
	public String ENCRYPTION_FIELD() {
		try {
			String encryption = PropString(PROP_ENCRYPTION);
			if (!UtilString.isEmpty(encryption)) {
				return UtilString.Split(encryption, ";", 1);
			}
		} catch (Exception ignored) {
		}
		return "1234567890";
	}
	public int ENCRYPTION() {
		try {
			String encryption = PropString(PROP_ENCRYPTION);
			if (!UtilString.isEmpty(encryption)) {
				String data = UtilString.Split(encryption, ";", 0);
				if (data.equalsIgnoreCase("FileScrtySSO")) {
					return Define.FileScrtySSO;
				} else if (data.equalsIgnoreCase("EncryptDB")) {
					return Define.EncryptDB;
				}
			}
		} catch (Exception ignored) {}
		return Define.None;
	}
	public String REDIS_APIKEY() {
		return PropString(PROP_REDIS_APIKEY);
	}	
	public String REDIS_URL(int i) {
		return PropString(String.format("REDIS_URL%02d", i));
	}
	public boolean CREATE_MEMBER_ENABLE() {
		String data = PropString(PROP_CREATE_MEMBER_ENABLE);
        return "y".equalsIgnoreCase(data);
    }
	public boolean LOCAL_HR_ENABLE() {
		String data = PropString(PROP_LOCAL_HR_ENABLE);
        return "y".equalsIgnoreCase(data);
    }

	//	일배치 관련 필드들...
	final private static String PROP_FILE_DIR						=	"FILE_DIR";
	final private static String PROP_FILE_CHARSET					=	"FILE_CHARSET";
	final private static String PROP_FILE_DELIMITER					=	"FILE_DELIMITER";
	final private static String PROP_FILE_VALID_FILE_SIZE			=	"FILE_VALID_FILE_SIZE";
	final private static String PROP_SOURCE_CHARSET					=	"SOURCE_CHARSET";
	final private static String PROP_TARGET_CHARSET					=	"TARGET_CHARSET";
	public String FILE_DIR() {
		String data = PropString(PROP_FILE_DIR);
		if(UtilString.isEmpty(data)) {
			return null;
		}
		return data;
	}
	public String FILE_CHARSET() {
		return PropString(PROP_FILE_CHARSET);
	}
	public String FILE_DELIMITER() {
		return PropString(PROP_FILE_DELIMITER);
	}
	public int FILE_MIN_FILE_BYTE() {
		String data = PropString(PROP_FILE_VALID_FILE_SIZE);
		if (!UtilString.isEmpty(data)) {
			if(data.contains("~")) {
				return Integer.parseInt(UtilString.Split(data.trim(), "~", 0));
			}
			return Integer.parseInt(data);
		}
		return 0;
	}
	public int FILE_MAX_FILE_BYTE() {
		String data = PropString(PROP_FILE_VALID_FILE_SIZE);
		if (!UtilString.isEmpty(data)) {
			if(data.contains("~")) {
				return Integer.parseInt(UtilString.Split(data.trim(), "~", 1));
			}
		}
		return -1;
	}
	public String SOURCE_CHARSET() {
		String data = PropString(PROP_SOURCE_CHARSET);
		if("NONE".equalsIgnoreCase(data)) {
			return null;
		}
		return data;
	}
	public String TARGET_CHARSET() {
		String data = PropString(PROP_TARGET_CHARSET);
		if("NONE".equalsIgnoreCase(data)) {
			return null;
		}
		return data;
	}
	
	//	ALERT
	final private static String PROP_ALERT_NOTIFY_URL						=	"ALERT_NOTIFY_URL";
	final private static String PROP_ALERT_NOTIFY_TIMEOUT					=	"ALERT_NOTIFY_TIMEOUT";

	public String ALERT_NOTIFY_URL() {
		return PropString(PROP_ALERT_NOTIFY_URL);
	}
	public int    ALERT_NOTIFY_CONNTO() {
		String data = PropString(PROP_ALERT_NOTIFY_TIMEOUT);
		String[] split = data.split(";");
		if(split.length == 2) {
			return Integer.parseInt(split[0].trim());
		}
		return 5000;
	}
	public int    ALERT_NOTIFY_READTO() {
		String data = PropString(PROP_ALERT_NOTIFY_TIMEOUT);
		String[] split = data.split(";");
		if(split.length == 2) {
			return Integer.parseInt(split[1].trim());
		}
		return 5000;
	}
	
	//	USER_UPDATE
	final private static String PROP_USER_UPDATE_URL						=	"USER_UPDATE_URL";
	final private static String PROP_USER_UPDATE_TIMEOUT					=	"USER_UPDATE_TIMEOUT";

	public String USER_UPDATE_URL() {
		return PropString(PROP_USER_UPDATE_URL);
	}
	public int    USER_UPDATE_CONNTO() {
		String data = PropString(PROP_USER_UPDATE_TIMEOUT);
		String[] split = data.split(";");
		if(split.length == 2) {
			return Integer.parseInt(split[0].trim());
		}
		return 5000;
	}
	public int    USER_UPDATE_READTO() {
		String data = PropString(PROP_USER_UPDATE_TIMEOUT);
		String[] split = data.split(";");
		if(split.length == 2) {
			return Integer.parseInt(split[1].trim());
		}
		return 5000;
	}
	
	//	PHONE DO
	final private static String PROP_PHONE_DO_URL					=	"PHONE_DO_URL";
	final private static String PROP_PHONE_DO_TIMEOUT				=	"PHONE_DO_URL_TIMEOUT";
	
	public String PHONE_DO_URL() {
		return PropString(PROP_PHONE_DO_URL);
	}
	public int PHONE_DO_CONNTO() {
		String data = PropString(PROP_PHONE_DO_TIMEOUT);
		String[] split = data.split(";");
		if(split.length >= 2) {
			return Integer.parseInt(split[0].trim());
		}
		return 5000;
	}
	public int PHONE_DO_READTO() {
		String data = PropString(PROP_PHONE_DO_TIMEOUT);
		String[] split = data.split(";");
		if(split.length >= 2) {
			return Integer.parseInt(split[1].trim());
		}
		return 5000;
	}

	// REALTIME
	final private static String PROP_REALTIME_FILE_DIR				=	"REALTIME_FILE_DIR";
	final private static String PROP_REALTIME_FILE_CHARSET			=	"REALTIME_FILE_CHARSET";
	final private static String PROP_REALTIME_FILE_DELIMITER		=	"REALTIME_FILE_DELIMITER";
	final private static String PROP_REALTIME_FILE_FIELD			=	"REALTIME_FILE_FIELD";
	final private static String PROP_REALTIME_FILE_VALID_FILE_SIZE	=	"REALTIME_FILE_VALID_FILE_SIZE";
	final private static String PROP_REALTIME_FILE_DISABLE_TIME		=	"REALTIME_FILE_DISABLE_TIME";
	
	public String REALTIME_FILE_DIR() {
		String data = PropString(PROP_REALTIME_FILE_DIR);
		if(UtilString.isEmpty(data)) {
			return null;
		}
		return data;
	}
	public String REALTIME_FILE_CHARSET() {
		return PropString(PROP_REALTIME_FILE_CHARSET);
	}
	public String REALTIME_FILE_DELIMITER() {
		return PropString(PROP_REALTIME_FILE_DELIMITER);
	}
	public long REALTIME_FILE_DISABLE_START() {
		try {
			String data = PropString(PROP_REALTIME_FILE_DISABLE_TIME);
			if (UtilString.isEmpty(data)) {
				return -1;
			}
			String time = UtilString.Split(data, "~", 0);
			// 시간을 ':'로 분할하여 분과 초로 나눔
			String[] timeParts = time.split(":");
			int minutes = Integer.parseInt(timeParts[0]);
			int seconds = Integer.parseInt(timeParts[1]);
	
			// 총 시간을 밀리초로 변환
            return (minutes * 60L + seconds) * 1000;
		} catch (Exception ignored) {
		}
		return -1;
	}
	public long REALTIME_FILE_DISABLE_END() {
		try {
			String data = PropString(PROP_REALTIME_FILE_DISABLE_TIME);
			if (UtilString.isEmpty(data)) {
				return -1;
			}
			String time = UtilString.Split(data, "~", 1);
			// 시간을 ':'로 분할하여 분과 초로 나눔
			String[] timeParts = time.split(":");
			int minutes = Integer.parseInt(timeParts[0]);
			int seconds = Integer.parseInt(timeParts[1]);
	
			// 총 시간을 밀리초로 변환
            return (minutes * 60L + seconds) * 1000;
		} catch (Exception ignored) {
		}
		return -1;
	}
	public List<String> REALTIME_FILE_FIELD() {
		List<String> list = new ArrayList<>();
		String data = PropString(PROP_REALTIME_FILE_FIELD);
		String[] split = data.split("\\|");
		for (String d : split) {
			list.add(d.trim());
		}
		return list;
	}
	public int REALTIME_FILE_MIN_FILE_BYTE() {
		String data = PropString(PROP_REALTIME_FILE_VALID_FILE_SIZE);
		if (!UtilString.isEmpty(data)) {
			if(data.contains("~")) {
				return Integer.parseInt(UtilString.Split(data.trim(), "~", 0));
			}
			return Integer.parseInt(data);
		}
		return 0;
	}
	public int REALTIME_FILE_MAX_FILE_BYTE() {
		String data = PropString(PROP_REALTIME_FILE_VALID_FILE_SIZE);
		if (!UtilString.isEmpty(data)) {
			if(data.contains("~")) {
				return Integer.parseInt(UtilString.Split(data.trim(), "~", 1));
			}
		}
		if(UtilString.isEmpty(data)) { return 0; }
		return -1;
	}	
	
	List<DataTable> fileList = new ArrayList<>();
	public List<DataTable> TABLE_INFO() {
		try {
			this.fileList.clear();
			for( int i = 0; i < 32; i++) {
				String formData = String.format("FILE%02d", i);
				String data = PropString(formData);
				if(UtilString.isEmpty(data)) { break; }
				DataTable newData = new DataTable();
				String fileName = UtilString.Split(data, ";", 0);
				if(fileName.contains("%")) {
					int index = fileName.indexOf("%");
					int last  = fileName.lastIndexOf("%");
					if(index >= 0 && last >= 0 && index != last) {
						String format = fileName.substring( index+1, last);
						String str = UtilCalendar.getLongToString(format);
						newData.setName(fileName.replaceAll("%"+format+"%", str));
						newData.setFullPath(Conf.getInstance().FILE_DIR() + File.separator  + newData.getName());
					}
				} else {
					newData.setName( fileName );
					newData.setFullPath(Conf.getInstance().FILE_DIR() + File.separator  + fileName);
				}
				newData.setTable( UtilString.Split(data, ";", 1) );
				newData.setQueryField( UtilString.Split(data, ";", 2) );
				UtilLog.t(getClass(), newData.toString());
				this.fileList.add(newData);
			}
			return fileList;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return null;
	}

	// RESPONSE URL
	final private static String PROP_BULK_RESPONSE_URL		=	"BULK_RESPONSE_URL";			//	Token 응답 URL

	// HTTP SERVER
	final private static String PROP_SERVER_HTTP_PORT		=	"SERVER_HTTP_PORT";
	final private static String PROP_SERVER_HTTP_IP			=	"SERVER_HTTP_IP";
	final private static String PROP_SERVER_HTTP_TIMEOUT	=	"SERVER_HTTP_TIMEOUT";

	//	TESTER
	final private static String PROP_TESTER_PORT			=	"TESTER_PORT";
	final private static String PROP_TESTER_TIMEOUT			=	"TESTER_TIMEOUT";
	final private static String PROP_TESTER_SAMPLE_FILE		=	"TESTER_SAMPLE_FILE";
	final private static String PROP_TESTER_MAKER_FILE		=	"TESTER_MAKER_FILE";

	//	RESPONSE URL
	public String BULK_RESPONSE_URL() {
		return PropString(PROP_BULK_RESPONSE_URL);
	}

	// HTTP SERVER
	public int SERVER_HTTP_PORT() {
		return PropInt(PROP_SERVER_HTTP_PORT);
	}
	public String SERVER_HTTP_IP() {
		return PropString(PROP_SERVER_HTTP_IP);
	}
	public int SERVER_HTTP_TIMEOUT() 				{
		return PropInt(PROP_SERVER_HTTP_TIMEOUT);
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