package com.ecs.esp.u.com.tester;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;

public class FileInformation implements Comparable<FileInformation> {
	/******************************************************************************
	 * Data
	 ******************************************************************************/
//	public static final String MENU_HTML_FILE	=	"_menu.html";
	
	private String dir;
	private String file;
	private int    seq;
	private final String creationTime;
	private final String lastModifiedTime;
	
    @Override
    public int compareTo(FileInformation other) {
        return this.file.compareTo(other.getFile());
    }

	public FileInformation(String bDir, String file, int seq) {
		this.dir  = bDir;
		this.file = file;
		this.seq  = seq;
		this.lastModifiedTime = lastModifiedTime(this.getSourceFile());
		this.creationTime = creationTime(this.getSourceFile());
	}
	public String getCreationTime() {
		return this.creationTime;
	}
	public String getLastModifiedTime() {
		return this.lastModifiedTime;
	}
	public String lastModifiedTime(String filePaht) {
		Path file = Paths.get( filePaht);
		try {
			FileTime lastModifiedTime = (FileTime) Files.getAttribute(file, "lastModifiedTime");
		    String pattern = "yyyy-MM-dd HH:mm:ss";
		    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            return simpleDateFormat.format( new Date( lastModifiedTime.toMillis() ) );
		} catch (IOException e) {
			UtilLog.e(getClass(), e.toString());
		//	e.printStackTrace();
		}
		return "";
	}
	public String creationTime(String filePaht) {
		Path file = Paths.get( filePaht);
		try {
			FileTime creationTime = (FileTime) Files.getAttribute(file, "creationTime");
		    String pattern = "yyyy-MM-dd HH:mm:ss";
		    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(pattern);
            return simpleDateFormat.format( new Date( creationTime.toMillis() ) );
		} catch (IOException e) {
			UtilLog.e(getClass(), e.toString());
			//	e.printStackTrace();
		}
		return "";
	}
	public String getSourceFile() {
		return this.dir+ FileSystems.getDefault().getSeparator() +this.file;
	}
	public String getDir() {
		return dir;
	}
	public void setDir(String dir) {
		this.dir = dir;
	}
	public int getSeq() {
		return this.seq;
	}
	public void setSeq(int seq) {
		this.seq = seq;
	}
	public String getFile() {
		return file;
	}
	public void setFile(String file) {
		this.file = file;
	}
	@Override
	public String toString() {
		return "FileInformation [dir=" + dir + ", file=" + file + ", seq=" + seq + ", creationTime=" + creationTime
				+ ", lastModifiedTime=" + lastModifiedTime + "]";
	}
	public void print() {
		String logger = "";
		if(!UtilString.isEmpty(dir) && !UtilString.isEmpty(file)) {
			String filePath = String.format("%s%s%s",  dir, File.separator, file);
			logger += String.format("[%02d][%-96s]", seq, filePath);
		}
		logger += String.format("[%s][%s]", this.getCreationTime(), this.getLastModifiedTime());
		UtilLog.i(getClass(), logger);
	}
}
