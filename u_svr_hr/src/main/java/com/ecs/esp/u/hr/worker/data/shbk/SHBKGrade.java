package com.ecs.esp.u.hr.worker.data.shbk;

import com.ecs.base.db2.annotation.ECSData;
import com.ecs.base.db2.data.DataDB2;

public class SHBKGrade extends DataDB2 {

    @ECSData({"mapping"})
    public String site;

    @ECSData({"mapping"})
    public String site_nm;

    @ECSData({"mapping"})
    public String tenant;

    @ECSData({"mapping"})
    public String tenant_nm;

    @ECSData({"mapping"})
    public String grade_nm;

    @ECSData({"mapping"})
    public String grade_code;




}
