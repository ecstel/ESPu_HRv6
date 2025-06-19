package com.ecs.esp.u.hr.define;

public class Define {
	/********************************************************************
	 * Final Thread ID
	 ********************************************************************/
	public final static int TID_HTTP_SERVER					=	0x01;
	public final static int TID_TESTER						=	0x02;
	public final static int TID_REALTIME_FILE_MONITOR		=	0x03;	//	수정된 데이터만 실시간 배치
	public final static int TID_DAILYBAT_FILE_MONITOR		=	0x04;	//	일배치 처리
	public final static int TID_HDB_WORKER					=	0x05;
	public final static int TID_RDB_WORKER					=	0x06;
	public final static int TID_LDB_WORKER					=	0x07;
	public final static int TID_HRREST_WORKER				=	0x08;

	public final static int TIMER_HTTP_SERVER				=	0x11;
	public final static int TIMER_REALTIME_FILE_MONITOR		=	0x12;
	public final static int TIMER_DAILYBAT_FILE_MONITOR		=	0x13;

	public final static int None							=	0;
	public final static int FileScrtySSO					=	1;
	public final static int EncryptDB						=	2;
//	public final static int DB								=	3;
//	public final static int METHOD_DB						=	1;					//	DB처리방식
//	public final static int METHOD_FILE						=	2;					//	파일처리방식
	
	public final static String MULTIPLE_QUERY				=	"MULTIPLE_QUERY";	//	다수 질의문 ( GET_SORUCE_USER/DEPT/GRADE/POSITION )
	public final static String SINGLE_QUERY					=	"SINGLE_QUERY";		//	단수 질의문 ( GET_SOURCE_[사이트명] )
	public final static String PREFIX_FIELD					=	"GET_SOURCE";
	public final static String USER_TEL						=	"USER_TEL";
	public final static String TREE							=	"TREE";
	public final static String ROOT							=	"ROOT";

	public final static String COMPLETED					=	"COMPLETED";

//	public final static String USER_ID						=	"USER_NO";
//	public final static String USER_PW						=	"USER_PW";
//	public final static String PICTURE						=	"PICTURE";


	public final static String SITE_SHBK					=	"SHBK";
}