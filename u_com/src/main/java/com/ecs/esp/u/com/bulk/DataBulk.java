package com.ecs.esp.u.com.bulk;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.annotation.ECSData;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.json.UtilJson;
import com.ecs.msg.comm.ECSMessage;
import com.ecs.msg.rest.custom.RESTMessage;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class DataBulk extends DataDB2 {
	/************************************************************************
	 * Data
	 ************************************************************************/
	@JsonFormat
	@ECSData({"mapping"})
	public String title;
	
	@ECSData({"mapping"})
	public String description = "";
	
	@JsonFormat
	@ECSData({"mapping"})
	public String result;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String cause;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String displayText;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String file_nm = "";
	
	@JsonFormat
	@ECSData({"mapping"})
	public String req_url;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String req_user;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String token;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String cmplt_dt;
	
	@JsonFormat
	@ECSData({"mapping"})
	public String status = "COMPLETE";
	
	//	ECSS 연동시..
	@ECSData({"mapping"})
	public String sender;
	
	@ECSData({"mapping"})
	public String alarmId;
	
	@ECSData({"mapping"})
	public String alarmMsg;
	
	@ECSData({"mapping"})
	public boolean dailyBatch = false;

	public Map<String, String> map = null;
	
	/************************************************************************
	 * Constructor
	 ************************************************************************/
	public DataBulk() { 
	}
	public DataBulk(EnumBulk bulk) {
		try {
			this.title			= bulk.getTitle();
			this.req_url		= bulk.getURL();
			this.cmplt_dt 		= UtilCalendar.getLongToString("yyyyMMddHHmmss");
		} catch (Exception e) {
			UtilLog.e(getClass(), e.toString());
		}
	}
	public DataBulk(RESTMessage msg) {
		try {
			this.token			= msg.GetParamJson("TOKEN");
			this.title			= msg.GetParamJson("TITLE");
			this.req_url		= msg.GetParamJson("REQ_URL");
			this.req_user       = msg.GetParamJson("LOGIN_ID");
			this.sender         = msg.GetParamJson("LOGIN_ID");
			this.alarmId        = msg.GetParamJson("ALARM_ID");
			this.cmplt_dt 		= UtilCalendar.getLongToString("yyyyMMddHHmmss");
		} catch (Exception e) {
			UtilLog.e(getClass(), e.toString());
		}	
	}
	public void setResult(boolean result) {
		if(result) {
			this.result = ECSMessage.STR_RESULT_SUCCESS;
			setCause(BulkResult.BULK_NONE);
		} else {
			this.result = ECSMessage.STR_RESULT_FAIL;
		}
	}
	public void setCause(int cause) {
		this.cause = String.valueOf(cause);
	}
	public void setCause(String cause) {
		this.cause = cause;
	}
	
	public void makeDisplayText() {
		if("SUCCESS".equalsIgnoreCase(this.result)) {
			this.displayText =  String.format("%s %s", this.title, "성공");
			this.description = this.displayText;
		}
		if("FAIL".equalsIgnoreCase(this.result)) {
			this.displayText =  String.format("%s %s", this.title, "실패");
			this.description = this.displayText;
		}
	}
	public String getEncodeJson() {
		try {
			makeDisplayText();
			map = new HashMap<String, String>();
			Field[] field = getClass().getFields();
			for(Field f : field) {
				if (f.getAnnotation(JsonFormat.class) != null) {
					try {
						Object value = f.get(this);
						if(value == null) { value = ""; }
						map.put(f.getName().toUpperCase(), value.toString());
					} catch (Exception e) {
						UtilLog.e(getClass(), e.toString());
					}
				} 
			}
			return UtilJson.EncodeJson(map);
		} catch (Exception e) {
			UtilLog.e(getClass(), e.toString());
		}
		return "";
	}
	public String getEncodeJsonECSS() {
		try {
			map = new HashMap<String, String>();
			map.put("sender", this.sender);
			map.put("alarmId", this.alarmId);
			map.put("alarmMsg", this.displayText);
			map.put("token", this.token);
			return UtilJson.EncodeJson(map);
		} catch (Exception e) {
			UtilLog.e(getClass(), e.toString());
		}
		return "";
	}
	public boolean isDailyBatch() {
		return this.dailyBatch;
	}
	public void setDailyBatch(boolean data) {
		this.dailyBatch = data;
	}
	
	@Override
	public String toString() {
		return "DataBulk [title=" + title + ", description=" + description + ", result=" + result + ", cause=" + cause
				+ ", displayText=" + displayText + ", file_nm=" + file_nm + ", req_url=" + req_url + ", token=" + token
				+ ", cmplt_dt=" + cmplt_dt + ", sender=" + sender + ", alarmId=" + alarmId + ", alarmMsg=" + alarmMsg
				+ ", dailyBatch=" + dailyBatch + ", map=" + map + "]";
	}
}