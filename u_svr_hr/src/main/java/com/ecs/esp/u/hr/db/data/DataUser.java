package com.ecs.esp.u.hr.db.data;

import java.util.Map;

import com.ecs.base.comm.UtilString;

public class DataUser {

	public String userNo;
	public String dn		=	"";
	
	public DataUser() {		
	}
	public DataUser(Map<String, String> map) {
		if(!UtilString.isEmpty( map.get("USER_ID"))) {
			this.userNo = map.get("USER_ID");
		}
		if(!UtilString.isEmpty( map.get("ID"))) {
			this.userNo = map.get("ID");
		}
		if(!UtilString.isEmpty( map.get("USER_TEL"))) {
			this.dn = map.get("USER_TEL");
		}
		if(!UtilString.isEmpty( map.get("DEVICE"))) {
			this.dn = map.get("DEVICE");
		}
		if(!UtilString.isEmpty( map.get("DN"))) {
			this.dn = map.get("DN");
		}
	}

	public final String getUserNo() {
		return userNo;
	}

	public final void setUserNo(String userNo) {
		this.userNo = userNo;
	}

	public final String getDn() {
		return dn;
	}

	public final void setDn(String dn) {
		this.dn = dn;
	}
}
