package com.ecs.esp.u.com.bulk;

public class BulkResult {
    private boolean result;
    private String cause;

    public BulkResult(boolean result, String cause) {
    	this.result = result;
        this.cause = cause;
    }
    public BulkResult() {
    	this.result = false;
        this.cause  = BULK_NONE;
    }
    
    public void setResult(boolean result) { 
    	this.result = result; 
    }
    public void setCause(String cause) {
    	this.cause = cause;
    }
    public void setCause(String funcName, String cause) {
    	this.cause = String.format("{%s} %s", funcName, cause);
    }
    public boolean isResult() 	{ return this.result; 	}
    public String getCause()	{ return this.cause; 	}
    
    public final static String BULK_NONE						=	"에러없음";
    public final static String BULK_UNKNOWN						=	"잘 알려지지 않은 에러";
    public final static String BULK_DB_CONNECTION_FAIL			=	"데이터베이스 연결 실패";
	public final static String BULK_DB_QUERY_FAIL				=	"데이터베이스 질의 오류";
	public final static String BULK_DB_NO_FIELD					=	"데이터베이스 필드 미정의";
	public final static String BULK_NO_RESULTS_FOUND			=	"질의 결과 미존재";
    public final static String BULK_INVALID_DATA                =   "유효하지 않는 데이터";
    public final static String BULK_NOT_ENOUGH_USERS            =   "1사용자 수 미달";
}
