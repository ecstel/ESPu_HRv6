package com.ecs.esp.u.hr.worker.data.shbk;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class SHBKResponse {

    private int code;

    @JsonProperty("data")
    private List<SHBKUser> users;

    private String message;
    private String status;

    public SHBKResponse() {}

    // getters / setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }

    public List<SHBKUser> getUsers() { return users; }
    public void setUsers(List<SHBKUser> users) { this.users = users; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
