package com.ecs.esp.u.com.tester;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.ecs.base.comm.UtilCom;
import com.ecs.base.comm.log.UtilLog;

public class FileList {
	/********************************************************
	 * Data
	 ********************************************************/
	public int seq = 1;
	private String menuFile = "";
	private final List<FileInformation> list = new ArrayList<FileInformation>();
	
	public FileList(String menuFile) {
		this.menuFile = menuFile;
	}
	public FileList() {
	}
	public List<FileInformation> GetList() {
		return this.list;
	}
	public void GetListDir(String source, String pattern) {
		File dir = new File(source);
		File[] fileList = dir.listFiles();
		try {
			for (int i = 0; i < Objects.requireNonNull(fileList).length; i++) {
				File file = fileList[i];
				if (file.isFile()) {			
					String path     = file.getParent();
			    	String filename = file.getName();
			    	if(menuFile.equals(filename)) { continue; }		
			    	if(UtilCom.PatternMatcher(pattern, filename)) {
			    		FileInformation dataFile = new FileInformation(path, filename, seq++);
				     	UtilLog.t(getClass(), String.format("PATH=%s, FILE=%s, PATTERN=%s, %s / %s ", path, filename, pattern, dataFile.getCreationTime(), dataFile.getLastModifiedTime()));
				     	list.add(dataFile);
				   }
			    } else if (file.isDirectory()) {
			    	GetListDir(file.getCanonicalPath(), pattern);
				}
			}
		} catch(Exception ignored) {}
	}
}