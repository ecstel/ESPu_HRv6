package com.ecs.esp.u.hr.worker;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.json.UtilJson;
import com.ecs.base.socket.thread.proc.EThreadWorker;
import com.ecs.esp.u.com.define.CommonConstants;
import com.ecs.esp.u.hr.db.data.DataAlert;
import com.ecs.esp.u.hr.db.data.DataPhoneDo;
import com.ecs.esp.u.hr.db.data.DataUser;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.worker.comps.RestSender;

import java.util.HashMap;
import java.util.Map;

/******************************************************************************
 * HTTP(S)를 통해 Rest 통신을 수행하는 프로세스 
 * HR -> ALERT 전송
 * HR -> CEPM 전송
 * HR -> 기간계 인서 서버 전송
 ******************************************************************************/
public class SenderRestWorker extends EThreadWorker {

	/********************************************************************
	 * Data
	 ********************************************************************/
	private final RestSender sender;

	/********************************************************************
	 * Constructor
	 ********************************************************************/
	public SenderRestWorker(int threadID) {
		super(threadID);
		this.sender = new RestSender();
	}
	
	/********************************************************************
	 * Start
	 ********************************************************************/
	public boolean Start() {
		return true;
	}
	
	/********************************************************************
	 * Stop
	 ********************************************************************/
	public boolean Stop() {
		super.stop();
		sender.shutdownExecutor();
		return true;
	}

	/***************************************************************************
	 * sender 
	 ***************************************************************************/
	protected void sender(String prefix,  String url, Map<String, String> headMap, Map<String, String> bodyMap, int connTimeOut, int readTimeout) {
		if(this.sender!=null) {
			sender.send(prefix, url, headMap, bodyMap, connTimeOut, readTimeout);
		}
	}
	
	/***************************************************************************
	 * postAlertNotify
	 ***************************************************************************/
	private void postAlertNotify(DataAlert alert) {
		try {
			String url = Conf.getInstance().ALERT_NOTIFY_URL();
			if (!UtilString.isEmpty(url)) {
				String sender = alert.getSenderList().get(0);
				for (String receiver : alert.getReceiverList()) {
					Map<String, String> bodyMap = new HashMap<String, String>();
					bodyMap.put(CommonConstants.VAR_SITE, 		Conf.getInstance().SITE());
					bodyMap.put(CommonConstants.VAR_JOBID, 		UtilString.GetCreateRandom());
					bodyMap.put(CommonConstants.VAR_SENDER, 	sender);
					bodyMap.put(CommonConstants.VAR_RECEIVER, 	receiver);
					bodyMap.put(CommonConstants.VAR_TITLE, 		alert.getTitle());
					StringBuilder sb = new StringBuilder();
					sb.append(String.format("%s <br>", alert.getContent()));
					for (String data : alert.getContentList()) {
						sb.append(data).append("<br>");
					}
					bodyMap.put(CommonConstants.VAR_CONTENT,	sb.toString());
					UtilLog.t(getClass(), "[postAlertNotify] url = " + url + " " + UtilJson.EncodeJson(bodyMap));
					sender(alert.getTitle(),
							Conf.getInstance().ALERT_NOTIFY_URL(),
							null,
							bodyMap,
							Conf.getInstance().ALERT_NOTIFY_CONNTO(),
							Conf.getInstance().ALERT_NOTIFY_READTO());
				}
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "{postAlertNotify} " + e.getMessage());
		}
	}
	
	/***************************************************************************
	 * postPhoneDoNotify
	 ***************************************************************************/
	private void postPhoneDoNotify(DataPhoneDo phoneDo) {
		try {
			String url = Conf.getInstance().PHONE_DO_URL();
			if (!UtilString.isEmpty(url)) {
				Map<String, String> bodyMap = new HashMap<String, String>();
				bodyMap.put(CommonConstants.VAR_ID, 		phoneDo.getUserNo());
				bodyMap.put(CommonConstants.VAR_DEVICE, 	phoneDo.getDn());
				UtilLog.t(getClass(), "[postPhoneDoNotify] url = " + url + " " + UtilJson.EncodeJson(bodyMap));
				sender("PHONE DO 알림",
						Conf.getInstance().PHONE_DO_URL(),
						null,
						bodyMap,
						Conf.getInstance().PHONE_DO_CONNTO(),
						Conf.getInstance().PHONE_DO_READTO());
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "{postPhoneDoNotify} " + e.getMessage());
		}
	}

	/***************************************************************************
	 * postUserUpdate
	 ***************************************************************************/
	private void postUserUpdate(DataUser user) {
		try {
			String url = Conf.getInstance().USER_UPDATE_URL();
			if (!UtilString.isEmpty(url)) {
				Map<String, String> bodyMap = new HashMap<String, String>();
				bodyMap.put(CommonConstants.VAR_ID, 		user.getUserNo());
				bodyMap.put(CommonConstants.VAR_DEVICE,   	user.getDn());
				UtilLog.t(getClass(), "[postUserUpdate] url = " + url + " " + UtilJson.EncodeJson(bodyMap));
				sender("내선번호변경 알림",
						Conf.getInstance().USER_UPDATE_URL(),
						null,
						bodyMap,
						Conf.getInstance().USER_UPDATE_CONNTO(),
						Conf.getInstance().USER_UPDATE_READTO());
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "{postUserUpdate} " + e.getMessage());
		}
	}

	@Override
	protected void onRecvMsg(Object obj) {
		if (obj instanceof DataUser) {			
			postUserUpdate((DataUser)obj);			//	내선번호 변경 알림 처리(수협은행 타 서버 전송)
		}
		if (obj instanceof DataPhoneDo) {	
			postPhoneDoNotify((DataPhoneDo)obj);	//	CLABEL 알림 처리
		}
		if (obj instanceof DataAlert) {			
			postAlertNotify((DataAlert)obj);		//	내선번호 중복 알림 처리(메신저 전송)
		}
	}
	@Override
	protected void onInit() {}
	@Override
	protected void onExit() {}
	@Override
	protected void onTimer(int timerID) {}
}