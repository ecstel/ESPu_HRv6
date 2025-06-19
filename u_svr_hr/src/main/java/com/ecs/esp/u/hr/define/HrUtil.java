package com.ecs.esp.u.hr.define;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import org.mozilla.universalchardet.UniversalDetector;

import com.ecs.base.comm.log.UtilLog;

public class HrUtil {

	
	/**********************************************************************************************************
	 * detectFileCharset
	 * @param word
	 * @return
	 **********************************************************************************************************/
	public static void detectFileCharset(String fileName, String charset) throws IOException {
		if ("CHECK".equalsIgnoreCase(charset)) {
			File file = new File(fileName);
			byte[] buf = new byte[4096];
			FileInputStream fis = new FileInputStream(file);
			UniversalDetector detector = new UniversalDetector(null);
			int nread;
			while ((nread = fis.read(buf)) > 0 && !detector.isDone()) {
				detector.handleData(buf, 0, nread);
			}
			detector.dataEnd();
			String encoding = detector.getDetectedCharset();
			detector.reset();
			fis.close();
			String data = encoding != null ? encoding : "인코딩을 감지할 수 없음";
			UtilLog.i(HrUtil.class, "FILE="+ fileName + ", 감지된 인코딩 타입=> " + data);
			System.exit(0);
		}
	}
}
