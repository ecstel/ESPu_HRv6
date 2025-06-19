package com.ecs.esp.u.hr.worker.data.shbk;

import com.ecs.base.db2.annotation.ECSData;
import com.ecs.base.db2.data.DataDB2;



public class SHBKUser extends DataDB2 {

    @ECSData({"mapping"})
    public String user_no;

    @ECSData({"mapping"})
    public String user_nm;

    @ECSData({"mapping"})
    public String site;

    @ECSData({"mapping"})
    public String site_nm;

    @ECSData({"mapping"})
    public String tenant;

    @ECSData({"mapping"})
    public String tenant_nm;

    @ECSData({"mapping"})
    public String dept_code;

    @ECSData({"mapping"})
    public String grade_code;

    @ECSData({"mapping"})
    public String position_code;

    @ECSData({"mapping"})
    public String direct_number;

    @ECSData({"mapping"})
    public String mobile;

    @ECSData({"mapping"})
    public String dn;

    public String getUser_no() {
        return user_no;
    }

    public void setUser_no(String user_no) {
        this.user_no = user_no;
    }

    public String getUser_nm() {
        return user_nm;
    }

    public void setUser_nm(String user_nm) {
        this.user_nm = user_nm;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public String getSite_nm() {
        return site_nm;
    }

    public void setSite_nm(String site_nm) {
        this.site_nm = site_nm;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getTenant_nm() {
        return tenant_nm;
    }

    public void setTenant_nm(String tenant_nm) {
        this.tenant_nm = tenant_nm;
    }

    public String getDept_code() {
        return dept_code;
    }

    public void setDept_code(String dept_code) {
        this.dept_code = dept_code;
    }

    public String getGrade_code() {
        return grade_code;
    }

    public void setGrade_code(String grade_code) {
        this.grade_code = grade_code;
    }

    public String getPosition_code() {
        return position_code;
    }

    public void setPosition_code(String position_code) {
        this.position_code = position_code;
    }

    public String getDirect_number() {
        return direct_number;
    }

    public void setDirect_number(String direct_number) {
        this.direct_number = direct_number;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getDn() {
        return dn;
    }

    public void setDn(String dn) {
        this.dn = dn;
    }

    @Override
    public String toString() {
        return "RestUser{" +
                "user_no='" + user_no + '\'' +
                ", user_nm='" + user_nm + '\'' +
                ", site='" + site + '\'' +
                ", site_nm='" + site_nm + '\'' +
                ", tenant='" + tenant + '\'' +
                ", tenant_nm='" + tenant_nm + '\'' +
                ", dept_code='" + dept_code + '\'' +
                ", grade_code='" + grade_code + '\'' +
                ", position_code='" + position_code + '\'' +
                ", direct_number='" + direct_number + '\'' +
                ", mobile='" + mobile + '\'' +
                ", dn='" + dn + '\'' +
                '}';
    }
}
