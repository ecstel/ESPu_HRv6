package com.ecs.esp.u.hr.db.data;

import com.ecs.base.comm.UtilFile;

public class DataTable {
	/********************************************************
	 * data
	 ********************************************************/
	private String 	name;			//	파일명(XXXX.CVS,XXXX.XLSX,XXXX.TXT)
	private String 	table;			//	테이블명
	private String  fullPath;		//	fullPath 파일명
	private String  queryField;		//	질의테이블명

	/********************************************************
	 * constructor
	 ********************************************************/
	public DataTable() {
	}

	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getFileExtension() {
		return UtilFile.getFileExtension(this.name);
	}
	public String getTable() {
		return table;
	}
	public void setTable(String table) {
		this.table = table;
	}
	public void setFullPath(String fullPath) {
		this.fullPath = fullPath;
	}
	public String getFullPath() {
		return fullPath;
	}
	public void setQueryField(String queryField) {
		this.queryField = queryField;
	}
	public String getQueryField() {
		return this.queryField;
	}
	@Override
	public String toString() {
		return "DataTable [name=" + name + ", table=" + table + ", queryField=" + queryField + ", fullPath=" + fullPath + "]";
	}
}
