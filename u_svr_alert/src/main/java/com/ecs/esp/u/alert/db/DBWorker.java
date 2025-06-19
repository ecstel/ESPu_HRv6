package com.ecs.esp.u.alert.db;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.socket.thread.proc.EThreadWorkerPoolDB2;
import com.ecs.esp.u.alert.define.Conf;
import com.ecs.esp.u.alert.define.ConfCommon;
import com.ecs.esp.u.alert.define.Define;
import com.ecs.esp.u.alert.worker.data.SendResult;
import com.ecs.esp.u.com.define.CommonConstants;
import com.ecs.msg.rest.custom.RESTMessage;

/******************************************************
 * DBWorker
 *
 * @author khkwon
 ******************************************************/
public class DBWorker extends EThreadWorkerPoolDB2 {
	/****************************************************************
	 * Data
	 ****************************************************************/
	private final String 	queryFile;
	private DataSource  	dbData;

	/****************************************************************
	 * Constructor
	 ****************************************************************/
	public DBWorker(int threadID, String queryFile) {
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
				UtilLog.i(getClass(), "[DBWorker] " + dbm.GetConnString());
			}
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
	protected void insert(RESTMessage msg) {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(dbData.IsQuery(dbm, CommonConstants.FD_INSERT_ALERT_HISTORY)) {
				DataDB2 insData = new DataDB2();
				insData.SetData(CommonConstants.VAR_SITE,  msg.GetParamJson(RESTMessage.CODE_SITE));
				insData.SetData(CommonConstants.VAR_TITLE,  msg.GetParamJson(RESTMessage.CODE_TITLE));
				insData.SetData(CommonConstants.VAR_SENDER,  msg.GetParamJson(RESTMessage.CODE_SENDER));
				insData.SetData(CommonConstants.VAR_RECEIVER,  msg.GetParamJson(RESTMessage.CODE_RECEIVER));
				insData.SetData(CommonConstants.VAR_STATUS,  msg.GetParamJson(RESTMessage.CODE_STATUS));
				if(UtilString.isEmpty(msg.GetParamJson(RESTMessage.CODE_STATUS))) {
					insData.SetData(CommonConstants.VAR_STATUS,  Define.DEFAULT_STATUS);
				}
				if(UtilString.isEmpty(msg.GetParamJson(RESTMessage.CODE_LOGIN_ID))) {
					insData.SetData(CommonConstants.VAR_LOGIN_ID,  Define.DEFAULT_LOGIN_ID);
				} else {
					insData.SetData(CommonConstants.VAR_LOGIN_ID,  msg.GetParamJson(RESTMessage.CODE_LOGIN_ID));
				}
				String content = msg.GetParamJson(RESTMessage.CODE_CONTENT);
				insData.SetData(
						CommonConstants.VAR_CONTENT,
						(content != null && content.length() > Define.MAX_CONTENT_LEN)
								? content.substring(0, Define.MAX_CONTENT_LEN)
								: content
				);
				insData.SetData(CommonConstants.VAR_JOBID,  msg.GetParamJson(RESTMessage.CODE_JOBID));
				insData.SetData(CommonConstants.VAR_RESULT, msg.GetParamJson(RESTMessage.CODE_RESULT));
				insData.SetData(CommonConstants.VAR_CAUSE,  msg.GetParamJson(RESTMessage.CODE_CAUSE));
				dbData.Insert(dbm, CommonConstants.FD_INSERT_ALERT_HISTORY, insData);
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{insert} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.t(getClass(), "SetTransaction{insert} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
	}
	protected void update(SendResult msg) {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return;
		}
		try {
			dbData.SetTransaction(dbm, true);
			if(dbData.IsQuery(dbm, CommonConstants.FD_UPDATE_ALERT_HISTORY)) {
				dbData.Update(dbm, CommonConstants.FD_UPDATE_ALERT_HISTORY, msg);
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{update} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.t(getClass(), "SetTransaction{update} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
	}
	@Override
	protected void onRecvMsg(Object obj) {
		if(obj instanceof RESTMessage) {
			insert((RESTMessage) obj);
		}
		if(obj instanceof SendResult){
			update((SendResult) obj);
		}
	}    
	protected void onExit() {}
	protected void onInit() {}
	protected void onTimer(int arg0) {}
}