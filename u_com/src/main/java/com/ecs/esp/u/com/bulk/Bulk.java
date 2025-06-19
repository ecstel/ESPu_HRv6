package com.ecs.esp.u.com.bulk;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.http.client.UtilHttpClient;
import com.ecs.base.json.UtilJson;
import com.ecs.base.socket.thread.EThread;
import com.ecs.msg.rest.custom.RESTMessage;
import com.ecs.msg.rest.seq.SequenceData;
import com.ecs.msg.rest.seq.SequenceManager;

public class Bulk {
	/****************************************************************************************
	 * static
	 ****************************************************************************************/
	public static final String SQLBulkv2 = "UPDATE ESP_UC.E_BULKWORK SET "
			+ "	STATUS = 'COMPLETE', "
			+ "	TITLE = '%s', " 
			+ "	DESCRIPTION = '%s', " 
			+ "	FILE_NM = '%s', " 
			+ "	RESULT = '%s', " 
			+ "	CAUSE = '%s', " 
			+ "	CMPLT_DT = CURRENT_TIMESTAMP " 
			+ "WHERE TOKEN = '%s'";
	
	public static final String SQLBatchv2 = "INSERT INTO ESP_UC.E_BATCHWORK (ID, APP_URL, REQ_USER, STATUS, TITLE, DESCRIPTION, RESULT, CAUSE, YMD, REQ_DT) "
			+ "VALUES "
			+ "("
			+ " (SELECT COALESCE(MAX(ID), 0) + 1 FROM ESP_UC.E_BATCHWORK), '%s', '%s', '%s', '%s', '%s', '%s', '%s', CURRENT_DATE, CURRENT_TIMESTAMP"
			+ ")";

	public static DataBulk send(String url, int tid, boolean result, String cause, RESTMessage msg) {
		try {
			int sequence = msg.getSequence();
			SequenceData seqData = SequenceManager.getInstance().removeSocket(sequence);	
			if (seqData != null) {
				//	웹페이지에서의 동기화
				DataBulk webBulk 	= new DataBulk(msg);
				webBulk.setDailyBatch(false);
				webBulk.setResult(result);
				if (!result) {
					webBulk.setCause(cause);
				}
				webBulk.makeDisplayText();
				UtilLog.i(Bulk.class, "########################################################################################");
				UtilLog.i(Bulk.class, "# update the database with the processing results. ");
				UtilLog.i(Bulk.class, "# \n" + UtilJson.prettyJson( webBulk.getEncodeJson()) );
				UtilLog.i(Bulk.class, "########################################################################################");
				EThread.postMessage(tid, webBulk);	//	결과 DB 갱신 업데이트
				
				if(!UtilString.isEmpty(url)) { // Conf.getInstance().BULK_RESPONSE_URL())) {
					UtilLog.i(Bulk.class, "########################################################################################");
					UtilLog.i(Bulk.class, "# sender url : " + url + ", sequence=" + sequence );
					UtilLog.i(Bulk.class, "# \n" + UtilJson.prettyJson( webBulk.getEncodeJsonECSS()) );
					UtilLog.i(Bulk.class, "########################################################################################");
					UtilHttpClient.ClientHttpPostJson(url, webBulk.getEncodeJsonECSS());
				}
				return webBulk;
			} else {
				//	일배치 동기화
				DataBulk dailyBulk 	= new DataBulk(msg);
				dailyBulk.setDailyBatch(true);
				dailyBulk.setResult(result);
				if (!result) {
					dailyBulk.setCause(cause);
				}
				dailyBulk.makeDisplayText();
				UtilLog.i(Bulk.class, "########################################################################################");
				UtilLog.i(Bulk.class, "# update the database with the processing results. ");
				UtilLog.i(Bulk.class, "# \n" + UtilJson.prettyJson( dailyBulk.getEncodeJson()) );
				UtilLog.i(Bulk.class, "########################################################################################");
				EThread.postMessage(tid, dailyBulk);	
				return dailyBulk;
			}
		} catch (Exception e) {
			UtilLog.e(Bulk.class, e);
		}
		return null;
	}
}