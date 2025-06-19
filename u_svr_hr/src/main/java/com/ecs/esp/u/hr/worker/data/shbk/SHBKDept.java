package com.ecs.esp.u.hr.worker.data.shbk;

import com.ecs.base.db2.annotation.ECSData;

public class SHBKDept {

    @ECSData({"mapping"})
    public String site;

    @ECSData({"mapping"})
    public String site_nm;

    @ECSData({"mapping"})
    public String tenant;

    @ECSData({"mapping"})
    public String tenant_nm;

    @ECSData({"mapping"})
    public String dept_nm;

    @ECSData({"mapping"})
    public String dept_code;

    @ECSData({"mapping"})
    public String dept_parent_code;

    @ECSData({"mapping"})
    public String abbr_nm;

    @ECSData({"mapping"})
    public String branch_code;

    @ECSData({"mapping"})
    public String name_tree;

    @ECSData({"mapping"})
    public String code_tree;


}
