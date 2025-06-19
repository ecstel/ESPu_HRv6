package com.ecs.esp.u.hr.server;

import java.util.ArrayList;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.UtilSyncTime;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.server.EThreadServer;
import com.ecs.esp.u.com.bulk.EnumBulk;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.msg.rest.comm.JSONMessage;
import com.ecs.msg.rest.custom.RESTMessage;
import com.ecs.msg.rest.custom.RESTStringData;
import com.ecs.msg.rest.handler.RestStringDecoder;
import com.ecs.msg.rest.handler.RestStringEncoder;
import com.ecs.msg.rest.seq.SequenceData;
import com.ecs.msg.rest.seq.SequenceManager;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpResponseStatus;

public class RestServerWorker extends EThreadServer {	
	/********************************************************************
	 * Final
	 ********************************************************************/
	public final static String RECV_MESSAGE_CST 	= "REST >>> ";
	public final static String SEND_MESSAGE_CST 	= "REST <<< ";

	/********************************************************************
	 * Data
	 ********************************************************************/
	private ArrayList<ChannelHandlerContext> ctxList = new ArrayList<ChannelHandlerContext>();

	/********************************************************************
	 * Construct
	 ********************************************************************/
	public RestServerWorker(int threadID, int port) {
		super(threadID, port, Conf.getInstance().SERVER_HTTP_TIMEOUT());
		
		setTimer(Define.TIMER_HTTP_SERVER, Conf.getInstance().SYNC_TIME_INTERVAL_START() * 1000, Conf.getInstance().SYNC_TIME_INTERVAL() * 1000);
	}
	protected void OnAccept(ChannelHandlerContext ctx) { 
		try {
			UtilLog.i(getClass(), "CONNECTED : " + ctx.channel().remoteAddress().toString());
			if(ctxList.indexOf(ctx) < 0) { ctxList.add(ctx); }
		} catch (Exception e) { }
	}
	protected void OnClose(ChannelHandlerContext ctx) { 
		try {
			UtilLog.i(getClass(), "DISCONNECTED : " + ctx.channel().remoteAddress().toString());
			ctx.close();
			if(ctxList.indexOf(ctx) >= 0) { ctxList.remove(ctx); }
		} catch (Exception e) { }
	}
	protected ChannelHandlerAdapter CreateEncoder() { return new RestStringEncoder(); }
	protected ChannelHandlerAdapter CreateDecoder() { return new RestStringDecoder(); }
	protected ArrayList<ChannelHandler> CreateChannelHandler() { return DefaultHttpHandler(this, 655360); }
	
	/********************************************************************
	 * Send
	 ********************************************************************/
	protected void Send(RESTStringData data, HttpResponseStatus status) {
		try {
			data.MESSAGE_CST = RESTStringData.SEND_MESSAGE_CST;
			data.respState = status;	
			Send(data.ctx, data);
		} catch(Exception e) { UtilLog.e(getClass(), e); }
	}
	
	/********************************************************************
	 * 정의되지 않은 필드값 강제로 설정
	 ********************************************************************/
	protected JSONMessage SetMessageData(JSONMessage msg) {
		switch(msg.getMessageID()) {
		case RESTMessage.REQ_REST_SYNC_INSA 	:
			msg.SetParamJson("TITLE",   EnumBulk.INSA_SYNC.getTitle());
			msg.SetParamJson("REQ_URL", EnumBulk.INSA_SYNC.getURL());
			return msg;
		case RESTMessage.REQ_REST_SYNC_PHONEDO 	:
			msg.SetParamJson("TITLE",   EnumBulk.PHONEDO_SYNC.getTitle());
			msg.SetParamJson("REQ_URL", EnumBulk.PHONEDO_SYNC.getURL());
			return msg;
		case RESTMessage.REQ_REST_USER 	:
			msg.SetParamJson("TITLE",   EnumBulk.USER.getTitle());
			msg.SetParamJson("REQ_URL", EnumBulk.USER.getURL());
			return msg;
		}
		return msg;
	}

	/********************************************************************
	 * @Override Netty
	 ********************************************************************/
	protected void OnRecv(ChannelHandlerContext ctx, Object obj) {
		try {
			String token = null;
			if(obj == null) {return ;}
			if(obj instanceof RESTStringData data) {
                UtilLog.t(getClass(), "[RECEIVED DATA] METHOD=" + data.method + ", pathVariable=["+data.pathVariable+"] queryParameter=["+data.queryParameter+"] requestBody=[" + data.requestBody + "]");
				if(HttpMethod.OPTIONS.name().equals(data.method)) {
					Send(data,  HttpResponseStatus.OK);	//	재호출 요청하기 위함.
					return;
				}		
				JSONMessage msg = data.DecodeMessage();
				if(msg != null) {
					msg.MESSAGE_CST = String.format(RESTStringData.RECV_METHOD_CST, data.path, data.method);
					msg.Logger();

					switch(msg.getMessageID()) {
					case RESTMessage.REQ_REST_SYNC_INSA 	:
						token = msg.GetParamJson(RESTMessage.CODE_TOKEN);
						if(!UtilString.isEmpty(token)) {
							Send(data, HttpResponseStatus.OK);
						}
						SequenceManager.getInstance().insertSocket(SEND_MESSAGE_CST, msg, ctx);
						EThread.postMessage(Define.TID_RDB_WORKER, SetMessageData(msg));
						break;
//					case RESTMessage.REQ_REST_SYNC_PHONEDO	:
//						token = msg.GetParamJson(RESTMessage.CODE_TOKEN);
//						if(!UtilString.isEmpty(token)) {
//							Send(data, HttpResponseStatus.OK);
//						}
//						SequenceManager.getInstance().insertSocket(SEND_MESSAGE_CST, msg, ctx);
//						EThread.postMessage(Define.TID_HDB_WORKER, SetMessageData(msg));
//						break;	
					case RESTMessage.REQ_REST_USER :
						token = msg.GetParamJson(RESTMessage.CODE_TOKEN);
						if(!UtilString.isEmpty(token)) {
							Send(data, HttpResponseStatus.OK);
						}
						SequenceManager.getInstance().insertSocket(SEND_MESSAGE_CST, msg, ctx);
						EThread.postMessage(Define.TID_HDB_WORKER, SetMessageData(msg));
						break;	
						
					default :
						Send(data,  HttpResponseStatus.BAD_REQUEST);	//	400 Bad Request, 요청이 정상적이지 않음. API에서 정의되지 않은 요청이 들어옴.
					}
				} else {					
					/** Send Response **/
					Send(data,  HttpResponseStatus.NOT_FOUND);
				}
			}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
	}

	/********************************************************************
	 * @Override EThread
	 ********************************************************************/
	protected void onRecvMsg(Object obj) {
		try {
			if(obj instanceof RESTMessage msg) {
                msg.MESSAGE_CST = RESTStringData.SEND_MESSAGE_CST;

				if(RESTMessage.isResponseMessage(msg.getMessageID())) {
					SequenceData data = SequenceManager.getInstance().removeSocket(msg);
					if(data != null) {
						EThreadServer.Send(data.ctx, msg);
					}
				} 
			}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
	}
	
	/****************************************************************************************************
	 * syncComponent : 동기화 처리
	 ****************************************************************************************************/
	private void syncComponent(int reqTID, int requestType, String logTag, String... keyValuePairs) {
		try {
		    RESTMessage message = new RESTMessage(requestType);
		    message.method      = HttpMethod.GET;
		    message.SetParamJson(RESTMessage.CODE_LOGIN_ID, "SYSTEM");
		    message.SetParamJson(RESTMessage.CODE_TOKEN, 	UtilString.GetCreateRandom());
		    if (keyValuePairs != null && keyValuePairs.length % 2 == 0) {
	            for (int i = 0; i < keyValuePairs.length; i += 2) {
	                String key = keyValuePairs[i];
	                String value = keyValuePairs[i + 1];
	                message.SetParamJson(key, value);
	            }
	        }
		    postMessage(reqTID, message);
		    UtilLog.i(getClass(), "[" + logTag + "] TOKEN=" + message.GetParamJson(RESTMessage.CODE_TOKEN));
		} catch(Exception e) { UtilLog.e(getClass(), e); }
	}
	
	UtilSyncTime syncTimer = new UtilSyncTime("HH:mm");
	@Override
	protected void onTimer(int timerID) {
		try {
			syncTimer.updateCheckTime(Conf.getInstance().SYNC_TIME());
			if (syncTimer.checkSync()) {
				UtilLog.i(getClass(), "#############################################################");
				UtilLog.i(getClass(), "#			인사동기화 자동시작								   ");
				UtilLog.i(getClass(), "#############################################################");

				syncComponent(Define.TID_RDB_WORKER,	
						RESTMessage.REQ_REST_SYNC_INSA,	
						"[일배치 동기화][" + EnumBulk.INSA_SYNC.getTitle() + "]",
						"TITLE",	EnumBulk.INSA_SYNC.getTitle(),
						"REQ_URL",  EnumBulk.INSA_SYNC.getURL());
				
				syncTimer.waitSync();
			}
		} catch(Exception e) { UtilLog.e(getClass(), e); }
	}
	protected void onInit() { }
	protected void onExit() { }
}
