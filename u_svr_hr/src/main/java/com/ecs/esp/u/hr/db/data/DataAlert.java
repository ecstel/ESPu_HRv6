package com.ecs.esp.u.hr.db.data;

import java.util.ArrayList;
import java.util.List;

public class DataAlert {

	/*****************************************************************
	 * Data
	 *****************************************************************/
	private String       title		= null;
	private String 		content		= null;
	private final List<String> senderList   = new ArrayList<String>();
	private final List<String> receiverList = new ArrayList<String>();
	private final List<String> contentList  = new ArrayList<String>();

	public DataAlert() {
	}
	public DataAlert(String title) {
		this.title = title;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
    public List<String> getSenderList() {
        return senderList;
    }

    public List<String> getReceiverList() {
        return receiverList;
    }

    public List<String> getContentList() {
        return contentList;
    }

	@Override
	public String toString() {
		return "DataAlert [senderList=" + senderList + ", receiverList=" + receiverList + ", contentList=" + contentList
				+ "]";
	}
}