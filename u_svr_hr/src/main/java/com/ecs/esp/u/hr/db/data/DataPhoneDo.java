package com.ecs.esp.u.hr.db.data;

import java.util.Map;

import com.ecs.base.comm.UtilString;

public class DataPhoneDo {
	
	
	public String userNo;
	public String dn		=	"";
	
	public DataPhoneDo() {		
	}
	public DataPhoneDo(Map<String, String> map) {
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
	
//	/***********************************************************************
//	 * Data
//	 ***********************************************************************/
//	public String user_no;
//	public String dn;
//	public int    sequence;
//	public String loginId;
//	public String alarmId;
//	
//	public DataPhoneDo(String user_no, String dn, int sequence, String loginId, String alarmId) {
//		this.user_no = user_no;
//		this.dn      = dn;
//		this.sequence = sequence;
//		this.loginId = loginId;
//		this.alarmId = alarmId;
//	}
//
//	public DataPhoneDo(String user_no, String dn) {
//		this.user_no = user_no;
//		this.dn      = dn;
//	}
//	public final String getUser_no() {
//		return user_no;
//	}
//	public final void setUser_no(String user_no) {
//		this.user_no = user_no;
//	}
//	public final String getDn() {
//		return dn;
//	}
//	public final void setDn(String dn) {
//		this.dn = dn;
//	}
//	@Override
//	public String toString() {
//		return "DataPhoneDo [user_no=" + user_no + ", dn=" + dn + "]";
//	}
}