package com.ecs.esp.u.com.define;

@SuppressWarnings("ALL")
public final class CommonConstants {

    private CommonConstants() { /* 인스턴스 생성 방지 */ }

    //  데이터베이스 질의문에 사용하는 필드명 생성 : FD_XXXXX
    public final static String FD_INSERT_BATCH 				= 	"INSERT_BATCH";
    public final static String FD_UPDATE_BULK 				= 	"UPDATE_BULK";
    public final static String FD_CLEAR						=	"CLEAR";
    public final static String FD_UPDATE					=	"UPDATE";
    public final static String FD_IMAGE						=	"IMAGE";
    public final static String FD_INSERT_ALERT_HISTORY	    =	"INSERT_ALERT_HISTORY";
    public final static String FD_UPDATE_ALERT_HISTORY	    =	"UPDATE_ALERT_HISTORY";
    public final static String FD_DISABLE_SITE				=	"DISABLE_SITE";
    public final static String FD_MERGE_SITE				=	"MERGE_SITE";
    public final static String FD_DISABLE_TENANT			=	"DISABLE_TENANT";
    public final static String FD_MERGE_TENANT				=	"MERGE_TENANT";
    public final static String FD_DISABLE_DIVISION			=	"DISABLE_DIVISION";
    public final static String FD_MERGE_DIVISION			=	"MERGE_DIVISION";
    public final static String FD_DISABLE_TEAM				=	"DISABLE_TEAM";
    public final static String FD_MERGE_TEAM				=	"MERGE_TEAM";
    public final static String FD_DISABLE_DEPT				=	"DISABLE_DEPT";
    public final static String FD_GET_CURRENT_DEPT			=	"GET_CURRENT_DEPT";
    public final static String FD_GET_DEPT					=	"GET_DEPT";
    public final static String FD_INSERT_DEPT				=	"INSERT_DEPT";
    public final static String FD_UPDATE_DEPT				=	"UPDATE_DEPT";
    public final static String FD_DISABLE_GRADE				=	"DISABLE_GRADE";
    public final static String FD_MERGE_GRADE				=	"MERGE_GRADE";
    public final static String FD_DISABLE_POSITION			=	"DISABLE_POSITION";
    public final static String FD_MERGE_POSITION			=	"MERGE_POSITION";
    public final static String FD_MERGE_USER_SERVICE		=	"MERGE_USER_SERVICE";
    public final static String FD_DISABLE_USER				=	"DISABLE_USER";
    public final static String FD_MERGE_USER				=	"MERGE_USER";
    public final static String FD_MERGE_MEMBER				=	"MERGE_MEMBER";
    public final static String FD_DEL_MEMBER_STATE			=	"DEL_MEMBER_STATE";
    public final static String FD_UPDATE_USER_LOGIN_FL		=	"UPDATE_USER_LOGIN_FL";
    public final static String FD_UPDATE_DN					=	"UPDATE_DN";				//	내선번호 갱신(HDBWorker)
    public final static String FD_DUPLICATE_DN				=	"DUPLICATE_DN";				//	내선 중복 체크(HDBWorker)
    public final static String FD_INSERT_HISTORY            =   "INSERT_HISTORY";
    public final static String FD_DEVICE					=	"DEVICE";
    public final static String FD_EXT						=	"EXT";
    public final static String FD_OUT						=	"OUT";
    public final static String FD_CUST						=	"CUST";
    public final static String FD_INSERT_MCID_FAIL			=	"INSERT_MCID_FAIL";

    //  데이터베이스 질의시 사용할 변수명 생성 : VAR_XXXX
    public final static String VAR_MEMID					=	"MEMID";
    public final static String VAR_USER_ID					=	"USER_ID";
    public final static String VAR_USER_NO					=	"USER_NO";
    public final static String VAR_USER_PW                  =   "USER_PW";
    public final static String VAR_USER_NM                  =   "USER_NM";
    public final static String VAR_DUPUSER                  =   "DUPUSER";
    public final static String VAR_PICTURE					=	"PICTURE";
    public final static String VAR_PICURL					=	"PICURL";
    public final static String VAR_SITE						=	"SITE";				//	사이트, 청
    public final static String VAR_SITE_NM					=	"SITE_NM";
    public final static String VAR_TENANT					=	"TENANT";			//	테넌트, 서
    public final static String VAR_TENANT_NM				=	"TENANT_NM";
    public final static String VAR_DIVISION					=	"DIVISION";			//	과
    public final static String VAR_DIVISION_NM				=	"DIVISION_NM";
    public final static String VAR_TEAM						=	"TEAM";				//	팀
    public final static String VAR_TEAM_NM					=	"TEAM_NM";
    public final static String VAR_DEPT_ID					=	"DEPT_ID";			//	부서
    public final static String VAR_DEPT_NM					=	"DEPT_NM";
    public final static String VAR_NAME_TREE				=	"NAME_TREE";
    public final static String VAR_CODE_TREE				=	"CODE_TREE";
    public final static String VAR_DEPT_CODE				=	"DEPT_CODE";
    public final static String VAR_BRANCH_CODE				=	"BRANCH_CODE";
    public final static String VAR_ABBR_NM					=	"ABBR_NM";
    public final static String VAR_DEPT_PARENT_CODE			=	"DEPT_PARENT_CODE";
    public final static String VAR_PARENT_ID				=	"PARENT_ID";
    public final static String VAR_DEPTH					=	"DEPTH";
    public final static String VAR_GRADE_NM					=	"GRADE_NM";
    public final static String VAR_GRADE_CODE				=	"GRADE_CODE";
    public final static String VAR_POSITION_NM				=	"POSITION_NM";
    public final static String VAR_POSITION_CODE			=	"POSITION_CODE";
    public final static String VAR_MCID_FL					=	"MCID_FL";
    public final static String VAR_JOBID                    =   "JOBID";
    public final static String VAR_SENDER                   =   "SENDER";
    public final static String VAR_RECEIVER                 =   "RECEIVER";
    public final static String VAR_TITLE                    =   "TITLE";
    public final static String VAR_CONTENT                  =   "CONTENT";
    public final static String VAR_ID                       =   "ID";
    public final static String VAR_DEVICE                   =   "DEVICE";
    public final static String VAR_STATUS                   =   "STATUS";
    public final static String VAR_RESULT                   =   "RESULT";
    public final static String VAR_CAUSE                    =   "CAUSE";
    public final static String VAR_LOGIN_ID                 =   "LOGIN_ID";
    public final static String VAR_DN                       =   "DN";
    public final static String VAR_USER_TEL                 =   "USER_TEL";
    public final static String VAR_TYPE                     =   "TYPE";
    public final static String VAR_CALLING_DEVICE			=	"CALLING_DEVICE";
    public final static String VAR_CALLED_DEVICE			=	"CALLED_DEVICE";
}
