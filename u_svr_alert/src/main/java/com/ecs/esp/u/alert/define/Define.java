package com.ecs.esp.u.alert.define;

import com.ecs.base.comm.UtilString;
import com.ecs.msg.rest.custom.RESTMessage;

public class Define {
	/********************************************************************
	 * Final Thread ID
	 ********************************************************************/
	public final static int TID_HTTP_SERVER 	= 	0x01;
	public final static int TID_TESTER 			= 	0x02;
	public final static int TID_DB_WORKER 		= 	0x03;
	public final static int TID_ALERT_WORKER 	= 	0x04;

	public final static int TIMER_HTTP_SERVER 	= 	0x11;

	public final static String SITE_ECS			= 	"ECS";
	public final static String SITE_KBSC		= 	"KBSC";
	public final static String SITE_SHBK		= 	"SHBK";


	public final static String DEFAULT_LOGIN_ID	= 	"SYSTEM";
	public final static String DEFAULT_STATUS	= 	"NONE";
	public final static int MAX_CONTENT_LEN		=	512;
	public final static String COMPLETED		=	"COMPLETED";
}