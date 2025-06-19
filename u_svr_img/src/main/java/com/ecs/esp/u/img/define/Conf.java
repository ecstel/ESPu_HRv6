package com.ecs.esp.u.img.define;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ecs.base.comm.EProperties;
import com.ecs.base.comm.UtilCom;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.esp.u.img.db.data.CfgImage;

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



	final private static String PROP_DOWNLOAD_DIRECTORY		=	"DOWNLOAD_DIRECTORY";
	final private static String PROP_PIC_WIDTH				=	"PIC_WIDTH";
	final private static String PROP_PIC_HEIGHT				=	"PIC_HEIGHT";
	final private static String PROP_FILE_SIZE				=	"FILE_SIZE";
	final private static String PROP_THUMBNAIL_DIRECTORY	=	"THUMBNAIL_DIRECTORY";
	final private static String PROP_THUMBNAIL_DEFAULT		=	"THUMBNAIL_DEFAULT";
	final private static String PROP_THUMBNAIL_EMPTY		=	"THUMBNAIL_EMPTY";
	final private static String PROP_THUMBNAIL_WIDTH		=	"THUMBNAIL_WIDTH";
	final private static String PROP_THUMBNAIL_HEIGHT		=	"THUMBNAIL_HEIGHT";

	
	public String THUMBNAIL_DIRECTORY() {
		return PropString(PROP_THUMBNAIL_DIRECTORY);
	}
	public String THUMBNAIL_DEFAULT() {
		return PropString(PROP_THUMBNAIL_DEFAULT);
	}
	public String THUMBNAIL_EMPTY() {
		return PropString(PROP_THUMBNAIL_EMPTY);
	}
	public int THUMBNAIL_WIDTH() 				{ return PropInt(PROP_THUMBNAIL_WIDTH); 				}
	public int THUMBNAIL_HEIGHT()  				{ return PropInt(PROP_THUMBNAIL_HEIGHT); 				}
	public String DOWNLOAD_DIRECTORY()  	{ return PropString(PROP_DOWNLOAD_DIRECTORY); 	}
	public int PIC_WIDTH() 				 	{ return PropInt(PROP_PIC_WIDTH); 				}
	public int PIC_HEIGHT()  				{ return PropInt(PROP_PIC_HEIGHT); 				}
	public int FILE_SIZE()					{ return PropInt(PROP_FILE_SIZE);				}

	final private static List<CfgImage> cfgImageList = new ArrayList<CfgImage>();
	public void LoadCfgImage() {
		if(cfgImageList!=null) {
			cfgImageList.clear();
		}
		for(int i = 0; i < 12; i++) {
			String data = PropString(String.format("IMAGE_URL%02d", i)); 
			if(UtilString.isEmpty(data)) { return; }
		
			CfgImage newData = new CfgImage();
			newData.setUrl(data);
			String pattern = PropString(String.format("IMAGE_PAT%02d", i)); 
			String[] patt =  pattern.split(";");
			for(String p : patt) {
				newData.addPatt(UtilCom.pack(p));
			}
			Objects.requireNonNull(cfgImageList).add(newData);
			UtilLog.i(getClass(), newData.toString());
		}
	}
	public CfgImage CheckCfgImage(String data) {
		for(CfgImage img : cfgImageList) {
			for( String compile : img.getPatt()) {
				boolean isRet = UtilCom.PatternMatcher(compile, data);
				if(isRet) {
				//	UtilLog.i(getClass(), "compile="+compile+", data="+data+", img="+img.getUrl());
					return img; 
				}
			}
		}
		return null;
	}

	//	DB
	final private static String PROP_SYNC_TIME				=	"SYNC_TIME";
	final private static String PROP_QUERY_FILE				=	"QUERY_FILE";
	final private static String PROP_SYNC_TIME_INTERVAL		=	"SYNC_TIME_INTERVAL";
	final private static String PROP_DB_WORKER_ETHREADS		=	"DB_WORKER_ETHREADS";
	final private static String PROP_CREATE_TABLE			=	"CREATE_TABLE";
	final private static String PROP_CREATE_TABLE_DAY		=	"CREATE_TABLE_DAY";				//	yyyyMMdd 테이블 생성 기간 정의

	public String SYNC_TIME() 	{ return PropString(PROP_SYNC_TIME);			}
	public String QUERY_FILE() 	{ return PropString(PROP_QUERY_FILE);			}
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