package com.ecs.esp.u.hr.db.comps;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.mozilla.universalchardet.UniversalDetector;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.file.UtilCommaFile;
import com.ecs.esp.u.hr.define.Conf;

public class FileWork {
	/**********************************************************************************************************
	 * Data
	 **********************************************************************************************************/
	private final DataSource dbData;
	
	/**********************************************************************************************************
	 * Constructor
	 **********************************************************************************************************/
	public FileWork(DataSource dbData) {
		this.dbData = dbData;
	}

	/**********************************************************************************************************
	 * detectFileCharset : 파일의 charset 확인
	 **********************************************************************************************************/
	public void detectFileCharset(String fileName) throws IOException {
		if ("CHECK".equalsIgnoreCase(Conf.getInstance().FILE_CHARSET())) {
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
			UtilLog.i(getClass(), "FILE="+ fileName + ", 감지된 인코딩 타입=> " + data);
			System.exit(0);
		}
	}
	
	/**********************************************************************************************************
	 * insertSplitFile : 구분자로 된 파일 처리하여 테이블에 등록함(CVS, DAT, TXT)
	 * @param filePath 파일 전체 패스 + 파일명
	 * @param tableName 파일명
	 * @param splitBy 구분자
	 **********************************************************************************************************/
	public int insertSplitFile(EDBM dbm, String filePath, String tableName, String splitBy) throws Exception {
		int valListSize = 0;
		try {
			detectFileCharset(filePath);
		} catch (IOException e) {
			UtilLog.e(getClass(), e);
		}
		
		if (!UtilFile.exists(filePath)) {
			UtilLog.e(getClass(), "{insertSplitFile} FILE NOT FOUND. "+filePath);
			return -1;
		}
		if(UtilString.isEmpty(tableName)) {
			return -1;
		}
		String updateTime = "";
		try {
			updateTime = UtilCalendar.getLongToString("yyyy-MM-dd");
		} catch (Exception e1) {
			UtilLog.e(getClass(), e1);
		}
	
		try {
			int expectedSize = 0;
			UtilCommaFile csv = null; 
			if(!UtilString.isEmpty(splitBy)) {
				csv = new UtilCommaFile(filePath, "r", null, splitBy);
			} else {
				csv = new UtilCommaFile(filePath, "r", null);
			}
			List<String> cellList = null; 
			csv.setCharSet(Conf.getInstance().FILE_CHARSET());
			List<List<String>> dataList = csv.readerList();
			int fields = csv.getFirstCells().size();
			for(String data : dataList.get(0)) {		//	첫번째 라인의 값을 자동으로 읽어서 테이블 생성
				String[] tokens = data.split(splitBy);
				cellList = new ArrayList<>(Arrays.asList(tokens));
				expectedSize = cellList.size();			//	최상단 필드 수로 작업함.
			}
			for (int i = 1; i < dataList.size(); i++) {
				UtilLog.i(getClass(), String.format("  %05d", i) + " => "+   dataList.get(i).toString()    );
			}
			UtilLog.i(getClass(), "");
			UtilLog.i(getClass(), "TABLE[" + tableName + "] SIZE[" + fields + "] FIELD" + Objects.requireNonNull(cellList).toString());
			if(dbData.createTable(dbm, Conf.getInstance().HR_SCHEMA(), tableName, cellList)) {
				dbData.Execute(dbm, String.format("DELETE FROM %s", tableName));
				for (int i = 1; i < dataList.size(); i++) {
					UtilLog.t(getClass(), String.format("  %05d", i) + " => "+   dataList.get(i).toString()    );
					for(String data : dataList.get(i)) {
						String[] tokens = data.split(splitBy, -1);
						List<String> valList = new ArrayList<>(Arrays.asList(tokens));
						// 토큰 값이 "null"인 경우 빈 문자열로 변경
					    for (int j = 0; j < valList.size(); j++) {
					        if ("null".equalsIgnoreCase(valList.get(j))) {
					            valList.set(j, "");
					        }
					    }
						if (valList.size() < expectedSize) {	//	사이즈가 작으면 ""으로 채우고
						    while (valList.size() < expectedSize) {
						        valList.add("");
						    }
						} 
						// 만약 토큰의 개수가 예상보다 많으면, 초과된 토큰은 잘라낸다.
						else if (valList.size() > expectedSize) {
						    valList = new ArrayList<>(valList.subList(0, expectedSize));
						}						
						if(dbData.insertTable(dbm, tableName, cellList, valList, updateTime)) {
							UtilLog.t(getClass(), "insertSplitFile insertTable");
						}
						valListSize = i;
					}
				}
				return valListSize;
			} else {
				UtilLog.e(getClass(), "TABLE 생성에 실패하였습니다.(TABLE=" +  tableName +", FIELD="+ cellList.toString()+")");
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::insertSplitFile()} " + e.getMessage());}
		return valListSize;
	}
}