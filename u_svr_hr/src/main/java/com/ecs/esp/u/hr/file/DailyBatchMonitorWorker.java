package com.ecs.esp.u.hr.file;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.proc.EThreadWorker;
import com.ecs.esp.u.com.bulk.EnumBulk;
import com.ecs.esp.u.hr.db.data.DataTable;
import com.ecs.esp.u.hr.db.data.ListWrapper;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.msg.rest.custom.RESTMessage;

import io.netty.handler.codec.http.HttpMethod;

/******************************************************
 * 디렉토리를 실시간 감시해서 파일이 업로드되면 이를 처리하는 클래스
 ******************************************************/
public class DailyBatchMonitorWorker extends EThreadWorker {
	/********************************************************************
	 * Constructor
	 ********************************************************************/
	public DailyBatchMonitorWorker(int threadID) {
		super(threadID);
	}
	
	/********************************************************************
	 * Start
	 ********************************************************************/
	public boolean Start() {
		setTimer(Define.TIMER_DAILYBAT_FILE_MONITOR, Conf.getInstance().SYNC_TIME_INTERVAL_START() * 1000, Conf.getInstance().SYNC_TIME_INTERVAL() * 1000);
		return true;
	}
	
	/********************************************************************
	 * Stop
	 ********************************************************************/
	public boolean Stop() {
		super.stop();
		return true;
	}
	
	public boolean existFileNames(File[] fileList) {
		if (fileList == null) {
			return false;
		}
		File[] files = fileList;
		List<File> onlyFiles = new ArrayList<>();
		for (File file : files) {
			if (file.isFile()) {
				onlyFiles.add(file);
			}
		}
		if (onlyFiles.size() > 0) {
			return true;
		}
		return false;
	}
    
    public boolean compareTableAndFileNames(List<DataTable> tableList, File[] fileList) {
        if (tableList == null || fileList == null) {
            return false;
        }
        Set<String> tableNames = new HashSet<>();
        for (DataTable table : tableList) {
        	tableNames.add(table.getName());
        }
        File[] files = fileList;
        List<File> onlyFiles = new ArrayList<>();
        for (File file : files) {
            if (file.isFile()) {
            	onlyFiles.add(file);
            }
        }
        Set<String> fileNames = new HashSet<>();
        for (File file : onlyFiles) {
        	fileNames.add(file.getName());
        }
        return fileNames.containsAll(tableNames);
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
            
            if(existFileNames(files) == false) {
             	UtilLog.t(getClass(), "파일 목록이 존재하지 않습니다.");
             	return null;
            }
            boolean compare = compareTableAndFileNames(Conf.getInstance().TABLE_INFO(), files);
            if(!compare) {
            	UtilLog.e(getClass(), "파일은 존재하나 설정된 파일의 목록과 실제 디렉토리의 파일 목록이 일치하지 않습니다.");
            	return null;
			}
            if (files != null) {
            	long minByte = Conf.getInstance().FILE_MIN_FILE_BYTE();
                long maxByte = Conf.getInstance().FILE_MAX_FILE_BYTE();
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
									String fileNewDir = Conf.getInstance().FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
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
									String fileNewDir = Conf.getInstance().FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
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
	    		String fileNewDir = Conf.getInstance().FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");	
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
	 * loadFile
	 ********************************************************************/
	protected void loadFile() {
		UtilLog.t(getClass(), "######################### loadFile #########################");
		List<DataTable> list = new ArrayList<DataTable>();
		
		// 파일 목록 가져오기
		List<File> files = getFilesList(Conf.getInstance().FILE_DIR());
		if (files != null && files.size() > 0) {
			try {
				String fileNewDir = Conf.getInstance().FILE_DIR() + File.separator + UtilCalendar.getLongToString("yyyyMMdd");
				UtilFile.mkdirs( fileNewDir );
				for (File file : files) {
					UtilFile.copyFile(file.getAbsolutePath(), fileNewDir + File.separator + file.getName());
					UtilFile.remove( file.getAbsolutePath() );
				}
				for (File file : files) {
					String fileFull = fileNewDir + File.separator  + file.getName();
					for(DataTable data : Conf.getInstance().TABLE_INFO()) {
						if(data.getName().equals(file.getName())) {
							DataTable table = new DataTable();
							table.setFullPath(fileFull);
							table.setName(file.getName());
							table.setTable(data.getTable());
							table.setQueryField(data.getQueryField());
							UtilLog.t(getClass(), table.toString());
							list.add(table);
						}
					}
				}
				if(list.size() > 0 ) {
					UtilLog.i(getClass(), "################# 일배치 파일 존재 확인 ################# ["+ list.size()+"]");
					
					RESTMessage msg = new RESTMessage(RESTMessage.REQ_REST_SYNC_INSA);
					msg.method      = HttpMethod.GET;
					msg.SetParamJson(RESTMessage.CODE_LOGIN_ID, "SYSTEM");
					msg.SetParamJson(RESTMessage.CODE_TOKEN, 	UtilString.GetCreateRandom());
					msg.SetParamJson("TITLE",	 EnumBulk.INSA_SYNC.getTitle());
					msg.SetParamJson("REQ_URL",  EnumBulk.INSA_SYNC.getURL());
					
				//	List<DataTable> objects = new ArrayList<>(list);
					ListWrapper<DataTable> wrapped = new ListWrapper<DataTable>(list, msg);
					EThread.postMessage(Define.TID_LDB_WORKER, wrapped);
					
				//	EThread.postMessage(Define.TID_LDB_WORKER, list);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onRecvMsg(Object obj) {}
	protected void onInit() {}
	protected void onExit() {}
	protected void onTimer(int timerID) {
		loadFile();
	}
}
