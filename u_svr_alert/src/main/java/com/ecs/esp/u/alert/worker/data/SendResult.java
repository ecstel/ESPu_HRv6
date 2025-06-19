package com.ecs.esp.u.alert.worker.data;

import com.ecs.base.db2.annotation.ECSData;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.esp.u.alert.define.Define;

public class SendResult extends DataDB2 {
    @ECSData("mapping")
    private String jobid;

    @ECSData("mapping")
    private String result;

    @ECSData("mapping")
    private String cause;

    @ECSData("mapping")
    private String 		status;

    public SendResult(String jobid, String result, String cause) {
        this.jobid = jobid;
        this.result = result;
        this.cause = cause;
        this.status = Define.COMPLETED;
    }
    public void setJobId(String jobid) {
        this.jobid = jobid;
    }
    public String getJobId() {
        return jobid;
    }
    public String getResult() 	{
        return this.result;
    }
    public void setResult(String result) {
        this.result = result;
    }
    public void setCause(String cause) {
        this.cause = cause;
    }
    public String getCause()	{
        return this.cause;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getStatus()	{
        return this.status;
    }
}
