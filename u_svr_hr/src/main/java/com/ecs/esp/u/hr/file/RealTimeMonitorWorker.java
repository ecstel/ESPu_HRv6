package com.ecs.esp.u.hr.file;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.proc.EThreadWorker;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.esp.u.hr.define.HrUtil;

public class RealTimeMonitorWorker extends EThreadWorker {
	/********************************************************************
	 * Constructor
	 ********************************************************************/
	public RealTimeMonitorWorker(int threadID) {
		super(threadID);
	}
	
	/********************************************************************
	 * Start
	 ********************************************************************/
	public boolean Start() {
		setTimer(Define.TIMER_REALTIME_FILE_MONITOR, Conf.getInstance().SYNC_TIME_INTERVAL_START() * 1000, Conf.getInstance().SYNC_TIME_INTERVAL() * 1000);
		return true;
	}
	/********************************************************************
	 * Stop
	 ********************************************************************/
	public boolean Stop() {
		super.stop();
		return true;
	}
	
    public List<File> getFilesList(String directoryPath) {
    	
    	if(UtilString.isEmpty(directoryPath)) {
    		return null;
    	}
    	// 지정된 경로의 디렉토리 객체 생성
        File directory = new File(directoryPath);
        // 반환할 파일 목록을 저장할 리스트
        List<File> fileList = new ArrayList<>();

        // 디렉토리가 존재하고 실제로 디렉토리인지 확인
        if (directory.exists() && directory.isDirectory()) {
            // 디렉토리 내의 파일 및 디렉토리 목록 가져오기
            File[] files = directory.listFiles();

            if (files != null) {
            	long minByte = Conf.getInstance().REALTIME_FILE_MIN_FILE_BYTE();
                long maxByte = Conf.getInstance().REALTIME_FILE_MAX_FILE_BYTE();
                for (File file : files) {
                    // 파일인 경우에만 처리
					if (file.isFile()) {
						long fileSize = file.length();
						if (maxByte != -1) {
							if (fileSize >= minByte && fileSize < maxByte) {
								UtilLog.i(getClass(), "Process file: " + file.getAbsolutePath() + ", " + fileSize + " bytes");
								fileList.add(file);
							} else {
								try {
									UtilLog.i(getClass(), "Move file: " + file.getAbsolutePath() + ", " + fileSize + " bytes");
									String fileNewDir = Conf.getInstance().REALTIME_FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
									UtilFile.mkdirs(fileNewDir);
									UtilFile.copyFile(file.getAbsolutePath(), fileNewDir + File.separator + file.getName());
									UtilFile.remove(file.getAbsolutePath());
								} catch (Exception e) {
									UtilLog.e(getClass(), e);
								}
							}
						} else {
							if (minByte == 0) {					//	0인 경우 모두 허용
								UtilLog.i(getClass(), "Process file: " + file.getAbsolutePath() + ", " + fileSize + " bytes");
								fileList.add(file);
							} else if (fileSize >= minByte) {	//	
								UtilLog.i(getClass(), "Process file: " + file.getAbsolutePath() + ", " + fileSize + " bytes");
								fileList.add(file);
							} else {
								try {
									UtilLog.i(getClass(), "Move file: " + file.getAbsolutePath()  + ", " + fileSize + " bytes");
									String fileNewDir = Conf.getInstance().REALTIME_FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
									UtilFile.mkdirs(fileNewDir);
									UtilFile.copyFile(file.getAbsolutePath(), fileNewDir + File.separator + file.getName());
									UtilFile.remove(file.getAbsolutePath());
								} catch (Exception e) {
									UtilLog.e(getClass(), e);
								}
							}
						}
                    }
                }
            }
        }
        return fileList;
    }
    
    /********************************************************************
   	 * removeFile : 파일을 이동시킴
   	 ********************************************************************/
    protected void removeFile(String dir) {
    	try {
	    	List<File> files = getFilesList(dir); 
	    	if (files.size() > 0) {
	    		String fileNewDir = Conf.getInstance().REALTIME_FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");	
				UtilFile.mkdirs( fileNewDir );
				for (File file : files) {
					UtilFile.copyFile(file.getAbsolutePath(), fileNewDir + File.separator + file.getName());
					UtilFile.remove( file.getAbsolutePath() );
				}	    		
	    	}
    	} catch (Exception e) {
			e.printStackTrace();
		}
    }
    
    /********************************************************************
  	 * validTimeRemoveFile : 지정된 시각에는 파일 처리를 하지 않음.
  	 ********************************************************************/
    protected boolean validTimeRemoveFile() {
		/** 지정된 시각에 들어오는 파일은 처리하지 않고 예외처리함 **/
		long start = Conf.getInstance().REALTIME_FILE_DISABLE_START();
		long end   = Conf.getInstance().REALTIME_FILE_DISABLE_END();
		try {
			if(start > 0 && end > 0) {
				String time = UtilCalendar.getLongToString("HH:mm");
			    String[] timeParts = time.split(":");
			    int minutes = Integer.parseInt(timeParts[0]);
			    int seconds = Integer.parseInt(timeParts[1]);
			    // 총 시간을 밀리초로 변환
			    long totalMilliseconds = (minutes * 60 + seconds) * 1000;
				if(totalMilliseconds >= start &&   totalMilliseconds < end) {
					UtilLog.i(getClass(), "현재시각 time=" + time + ", seconds=" + totalMilliseconds + " [ " + start + " ~ " + end + " ] 은 미처리");
					removeFile(Conf.getInstance().REALTIME_FILE_DIR());
					return true;
				}
			}
		} catch (Exception e1) { UtilLog.e(getClass(), e1); }
		return false;
	}

    /********************************************************************
	 * loadFile
	 ********************************************************************/
	protected void loadFile() {
		UtilLog.t(getClass(), "######################### loadFile #########################");
		
		// 지정된 시각에는 파일을 처리하지 않음.
		if(validTimeRemoveFile()) {
			return;
		}
		
		// 파일 목록 가져오기
		List<File> files = getFilesList(Conf.getInstance().REALTIME_FILE_DIR());
		if (files.size() > 0) {
			try {
				String fileNewDir = Conf.getInstance().REALTIME_FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
				UtilFile.mkdirs( fileNewDir );
				for (File file : files) {
					UtilFile.copyFile(file.getAbsolutePath(), fileNewDir + File.separator + file.getName());
					UtilFile.remove( file.getAbsolutePath() );
				}

				List<String> dnList = new ArrayList<String>();
				for (File file : files) {
					String fileFull = fileNewDir + File.separator  + file.getName();
					List<Map<String, String>> dataList = readFileAndPrint(fileFull, Conf.getInstance().REALTIME_FILE_CHARSET(), Conf.getInstance().REALTIME_FILE_DELIMITER(), Conf.getInstance().REALTIME_FILE_FIELD());
					if (dataList != null && dataList.size() > 0) {
						for(Map<String, String> m : dataList) {
							UtilLog.i(getClass(), "FILE[ "+file.getName() + " ] => " + m.toString());
							String dn = m.get(Define.USER_TEL);
							dnList.add(dn);
						}
						if(!UtilString.isEmpty(Conf.getInstance().HDB_QUERY_FILE())) {
							EThread.postMessage(Define.TID_HDB_WORKER, dataList);	
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/********************************************************************
	 * readFileAndPrint
	 ********************************************************************/
	public List<Map<String, String>> readFileAndPrint(String filePath, String encoding, String delimiter,
			List<String> keys) {
		try {
			HrUtil.detectFileCharset(filePath, Conf.getInstance().REALTIME_FILE_CHARSET());
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		List<Map<String, String>> dataList = new ArrayList<>();
		File file = new File(filePath);

		if (file.exists() && file.isFile()) {
			try (BufferedReader reader = new BufferedReader(
					new InputStreamReader(new FileInputStream(file), Charset.forName(encoding)))) {
				String line;
				while ((line = reader.readLine()) != null) {
					Map<String, String> dataMap = new HashMap<>();
					String[] parts = line.split(delimiter);
					for (int i = 0; i < parts.length && i < keys.size(); i++) {
						dataMap.put(keys.get(i), parts[i].trim());
						UtilLog.t(getClass(), i + " " + keys.get(i) + "=" + parts[i].trim());
					}
					dataList.add(dataMap);
				}
			} catch (Exception e) {
				UtilLog.e(getClass(), "파일을 읽는 중 오류가 발생했습니다: " + e.getMessage());
			}
		} else {
			UtilLog.e(getClass(), "지정된 파일을 찾을 수 없습니다.");
		}
		return dataList;
	}

	@Override
	protected void onRecvMsg(Object obj) {}
	protected void onInit() {}
	protected void onExit() {}
	protected void onTimer(int timerID) {
		loadFile();
	}
}
