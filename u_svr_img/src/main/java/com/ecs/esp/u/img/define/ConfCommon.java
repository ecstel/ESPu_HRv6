package com.ecs.esp.u.img.define;

import com.ecs.base.db2.define.EPropDatabase;

public class ConfCommon extends EPropDatabase {
	/********************************************************************
	 * Instance
	 ********************************************************************/
	private static ConfCommon instance;
	public static ConfCommon getInstance() {
		if(instance == null) { instance = new ConfCommon(); }
		return instance;
	}
}
