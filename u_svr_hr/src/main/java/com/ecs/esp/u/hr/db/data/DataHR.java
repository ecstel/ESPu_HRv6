package com.ecs.esp.u.hr.db.data;

import java.lang.reflect.Field;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.annotation.ECSData;
import com.ecs.base.db2.data.DataDB2;

public class DataHR extends DataDB2 {	
	/*********************************************************************
	 * Data
	 *********************************************************************/
	@ECSData({"mapping", "print"})	
	public String 	site;
	@ECSData({"mapping", "print"})	
	public String 	tenant;
	@ECSData({"mapping", "print"})
	public String 	dept_nm;
	@ECSData({"mapping"})
	public String 	name_tree;
	@ECSData({"mapping"})
	public int    	parent_id;
	@ECSData({"mapping"})
	public int    	tenant_id;
	@ECSData({"mapping"})
	public int    	dept_id;
	@ECSData({"mapping", "print"})	
	public int    	dept_ord;
	@ECSData({"mapping", "print"})	
	public int    	depth;
	@ECSData({"mapping", "print"})
	public String 	abbr_nm;
	
	@ECSData({"mapping", "print"})	
	public String   dept_code;				//	사이트 DEPT CODE
	@ECSData({"mapping", "print"})	
	public String 	dept_parent_code;
	@ECSData({"mapping", "print"})	
	public String   branch_code;			//	BRANCH CODE
	
	public void mapping() {
		Field field[] = getClass().getDeclaredFields();	
		for (Field f : field) {
			if (f.getAnnotation(ECSData.class) instanceof ECSData) {
				final ECSData annotation = f.getAnnotation(ECSData.class);
				for (String data : annotation.value()) {
				 	if("mapping".equals(data)) {
						try {
							f.setAccessible(true);
							Object value = f.get(this);
							if (value == null) { value = ""; }
							String key = f.getName().toUpperCase();
							if(!UtilString.isEmpty(key) && value != null) {
								SetData(key, value.toString());
							}
						} catch (Exception e) {	UtilLog.e(getClass(), e);	}
					}
				}
			}
		}
	}
	@Override
	public String toString() {
		return "InputData [site=" + site + ", tenant=" + tenant + ", dept_nm=" + dept_nm + ", name_tree=" + name_tree
				+ ", parent_id=" + parent_id + ", tenant_id=" + tenant_id + ", dept_id=" + dept_id + ", dept_ord="
				+ dept_ord + ", dept_code=" + dept_code + ", branch_code=" + branch_code+", depth=" + depth + "]";
	}
}