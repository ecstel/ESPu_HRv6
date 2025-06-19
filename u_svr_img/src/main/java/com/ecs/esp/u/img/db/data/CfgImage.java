package com.ecs.esp.u.img.db.data;

import java.util.ArrayList;
import java.util.List;

public class CfgImage {

	public String url;
	public List<String> pattList = null;
	
	public CfgImage() {
        pattList = new ArrayList<String>();
    }
	public void setUrl(String data) {
		this.url = data;
	}
	public String getUrl() {
		return this.url;
	}
	public void addPatt(String data) {
		this.pattList.add(data);
	}
	public List<String> getPatt() {
		return this.pattList;
	}

	@Override
	public String toString() {
		return "CfgImage [url=" + url + ", pattList=" + pattList + "]";
	}
}
