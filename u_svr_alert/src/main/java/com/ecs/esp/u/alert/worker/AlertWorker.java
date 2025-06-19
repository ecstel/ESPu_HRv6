package com.ecs.esp.u.alert.worker;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.EThread;
import com.ecs.base.socket.thread.proc.EThreadWorker;
import com.ecs.esp.u.alert.define.Conf;
import com.ecs.esp.u.alert.define.Define;
import com.ecs.esp.u.alert.worker.comps.RestSender;
import com.ecs.msg.rest.custom.RESTMessage;
import com.ecs.msg.rest.handler.ECSMessageRest;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class AlertWorker extends EThreadWorker {
    /********************************************************************
     * Data
     ********************************************************************/
    private final RestSender sender;

    /********************************************************************
     * Constructor
     ********************************************************************/
    public AlertWorker(int threadID, int sizeQ) {
        super(threadID, sizeQ);
        this.sender = new RestSender();
    }

    /********************************************************************
     * Start
     ********************************************************************/
    public boolean Start() {
        return true;
    }

    /********************************************************************
     * Stop
     ********************************************************************/
    public boolean Stop() {
        super.stop();
        sender.shutdownExecutor();
        return true;
    }

    /***************************************************************************
     * sender
     ***************************************************************************/
    protected void sender(String prefix,
                          String jobid,
                          String site,
                          String url,
                          Map<String, String> headerMap,
                          Map<String, String> bodyMap,
                          int connTimeOut, int readTimeout)
    {
        if(this.sender != null) {
            sender.send(prefix, jobid, site, url, headerMap, bodyMap, connTimeOut, readTimeout);
        }
    }

    /**********************************************************
     * sendHttp : 결과 리턴 함수
     **********************************************************/
    protected void sendHttp(RESTMessage msg, String result, String cause) {
        ECSMessageRest resp = new ECSMessageRest(msg.getRESTRespMessageID());
        resp.AddHeaderMessageParams(msg);
        resp.ResponseRest(result, cause);
        EThread.postMessage(Define.TID_HTTP_SERVER, resp);
    }

    protected void sendDB(RESTMessage msg, String result, String cause) {
        if ("FAIL".equals(result)) {            //  실패만 바로 DB에 insert 시킴.
            msg.SetParamJson(RESTMessage.CODE_RESULT, result);
            msg.SetParamJson(RESTMessage.CODE_CAUSE,  cause);
        }
        EThread.postMessage(Define.TID_DB_WORKER, msg);
    }
    /*
     * KB증권
     */
    protected Boolean kbsc(RESTMessage msg){
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            Map<String, String> paramsMap = new HashMap<String, String>();
            String site   = msg.GetParamJson(RESTMessage.CODE_SITE);
            String jobId  = msg.GetParamJson(RESTMessage.CODE_JOBID);
            String title  = msg.GetParamJson(RESTMessage.CODE_TITLE);
            String url = Conf.getInstance().URL(site);
            if(UtilString.isEmpty(url)) {
                return false;
            }
            // HEADER 고정값
            for(int i = 0; i < 12; i++) {
                String headerKey = Conf.getInstance().HEADER_KEY(site, i);
                if(UtilString.isEmpty(headerKey)) {
                    break;
                }
                String headerVal = Conf.getInstance().HEADER_VAL(site, i);
                if(!UtilString.isEmpty(headerVal)) {
                    headerMap.put(headerKey, headerVal);
                }
            }
            // BODY 고정값
            for(int i = 0; i < 12; i++) {
                String paramKey = Conf.getInstance().PARAM_KEY(site, i);
                if(UtilString.isEmpty(paramKey)) {
                    break;
                }
                String paramVal = Conf.getInstance().PARAM_VAL(site, i);
                if(!UtilString.isEmpty(paramVal)) {
                    paramsMap.put(paramKey, paramVal);
                }
            }
            //	BODY 치환
            for(int i = 0; i < 12; i++) {
                String orgKey = Conf.getInstance().DATA_KEY(site, i);
                if(UtilString.isEmpty(orgKey)) {
                    break;
                }
                Map<String, String> map = msg.GetParamJsonStrList();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if(orgKey.equalsIgnoreCase( entry.getKey() )) {
                        String chgKey = Conf.getInstance().DATA_VAL(site, i);
                        paramsMap.put( chgKey, entry.getValue() );
                    }
                }
            }
            UtilLog.t(getClass(), " title = " + title + ", jobId  = " + jobId + ", URL = " + url);
            UtilLog.t(getClass(), " header = " + headerMap.toString());
            UtilLog.t(getClass(), " params = " + paramsMap.toString());
            sender(title,
                    jobId,
                    site,
                    url,
                    headerMap,
                    paramsMap,
                    Conf.getInstance().CONNTO(site),
                    Conf.getInstance().READTO(site));
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    /*
     * ECS 데모 
     */
    protected Boolean ecs(RESTMessage msg){
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            Map<String, String> paramsMap = new HashMap<String, String>();
            String site   = msg.GetParamJson(RESTMessage.CODE_SITE);
            String jobId  = msg.GetParamJson(RESTMessage.CODE_JOBID);
            String title  = msg.GetParamJson(RESTMessage.CODE_TITLE);
            String url = Conf.getInstance().URL(site);
            if(UtilString.isEmpty(url)) {
                return false;
            }
            // HEADER 고정값
            for(int i = 0; i < 12; i++) {
                String headerKey = Conf.getInstance().HEADER_KEY(site, i);
                if(UtilString.isEmpty(headerKey)) {
                    break;
                }
                String headerVal = Conf.getInstance().HEADER_VAL(site, i);
                if(!UtilString.isEmpty(headerVal)) {
                    headerMap.put(headerKey, headerVal);
                }
            }
            // BODY 고정값
            for(int i = 0; i < 12; i++) {
                String paramKey = Conf.getInstance().PARAM_KEY(site, i);
                if(UtilString.isEmpty(paramKey)) {
                    break;
                }
                String paramVal = Conf.getInstance().PARAM_VAL(site, i);
                if(!UtilString.isEmpty(paramVal)) {
                    paramsMap.put(paramKey, paramVal);
                }
            }
            //	BODY 치환
            for(int i = 0; i < 12; i++) {
                String orgKey = Conf.getInstance().DATA_KEY(site, i);
                if(UtilString.isEmpty(orgKey)) {
                    break;
                }
                Map<String, String> map = msg.GetParamJsonStrList();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if(orgKey.equalsIgnoreCase( entry.getKey() )) {
                        String chgKey = Conf.getInstance().DATA_VAL(site, i);
                        paramsMap.put( chgKey, entry.getValue() );
                    }
                }
            }
            UtilLog.t(getClass(), " title = " + title + ", jobId  = " + jobId + ", URL = " + url);
            UtilLog.t(getClass(), " header = " + headerMap.toString());
            UtilLog.t(getClass(), " params = " + paramsMap.toString());
            sender(title,
                    jobId,
                    site,
                    url,
                    headerMap,
                    paramsMap,
                    Conf.getInstance().CONNTO(site),
                    Conf.getInstance().READTO(site));
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    /*
     * 수협은행 연동 : 추후, 상황에 사이트에 따른 수정 가능
     */
    protected Boolean shbk(RESTMessage msg){
        try {
            Map<String, String> headerMap = new HashMap<String, String>();
            Map<String, String> paramsMap = new HashMap<String, String>();
            String site   = msg.GetParamJson(RESTMessage.CODE_SITE);
            String jobId  = msg.GetParamJson(RESTMessage.CODE_JOBID);
            String title  = msg.GetParamJson(RESTMessage.CODE_TITLE);
            String url = Conf.getInstance().URL(site);
            if(UtilString.isEmpty(url)) {
                return false;
            }
            // HEADER 고정값
            for(int i = 0; i < 12; i++) {
                String headerKey = Conf.getInstance().HEADER_KEY(site, i);
                if(UtilString.isEmpty(headerKey)) {
                    break;
                }
                String headerVal = Conf.getInstance().HEADER_VAL(site, i);
                if(!UtilString.isEmpty(headerVal)) {
                    headerMap.put(headerKey, headerVal);
                }
            }
            // BODY 고정값
            for(int i = 0; i < 12; i++) {
                String paramKey = Conf.getInstance().PARAM_KEY(site, i);
                if(UtilString.isEmpty(paramKey)) {
                    break;
                }
                String paramVal = Conf.getInstance().PARAM_VAL(site, i);
                if(!UtilString.isEmpty(paramVal)) {
                    paramsMap.put(paramKey, paramVal);
                }
            }
            //	BODY 치환
            for(int i = 0; i < 12; i++) {
                String orgKey = Conf.getInstance().DATA_KEY(site, i);
                if(UtilString.isEmpty(orgKey)) {
                    break;
                }
                Map<String, String> map = msg.GetParamJsonStrList();
                for (Map.Entry<String, String> entry : map.entrySet()) {
                    if(orgKey.equalsIgnoreCase( entry.getKey() )) {
                        String chgKey = Conf.getInstance().DATA_VAL(site, i);
                        paramsMap.put( chgKey, entry.getValue() );
                    }
                }
            }
            UtilLog.t(getClass(), " title = " + title + ", jobId  = " + jobId + ", URL = " + url);
            UtilLog.t(getClass(), " header = " + headerMap.toString());
            UtilLog.t(getClass(), " params = " + paramsMap.toString());
            sender(title,
                    jobId,
                    site,
                    url,
                    headerMap,
                    paramsMap,
                    Conf.getInstance().CONNTO(site),
                    Conf.getInstance().READTO(site));
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    /*
     * DB와 HTTP에 동일한 페이로드를 보내는 공통 메서드
     */
    private void sendResponse(RESTMessage msg, boolean success, String cause) {
        String resultCode = success ? RESTMessage.RESULT_SUCCESS : RESTMessage.RESULT_FAIL;
        sendDB(  msg, resultCode, cause);
        sendHttp(msg, resultCode, cause);
    }

    /*
     * 호출 처리 메인함수
     */
    protected void alert(RESTMessage msg) {
        try {
            String site = Optional.ofNullable(msg.GetParamJson(RESTMessage.CODE_SITE))
                    .map(String::toUpperCase)
                    .orElse("");

            if (site.isEmpty()) {
                UtilLog.e(getClass(), "SITE(" + site + ") 필드 값이 존재하지 않습니다.");
                // 사이트 정보가 없을 때
                sendResponse(msg, false, RESTMessage.CAUSE_BAD_DATA);
                return;
            }
            if(!Conf.getInstance().SITE().equalsIgnoreCase(site)) {
                UtilLog.e(getClass(), " SITE(" + site + ") 와 환경 설정값("+Conf.getInstance().SITE()+")이 서로 다릅니다.");
                sendResponse(msg, false, RESTMessage.CAUSE_UNDEFINED_DATA); //  받은 데이터값과 설정된 값이 다름.
                return;
            }
            if(UtilString.isEmpty( Conf.getInstance().URL(site) )) {
                UtilLog.e(getClass(), "SITE(" + site + ") 와 관련된 기본 설정값("+site+"_URL) 이 존재하지 않습니다.");
                sendResponse(msg, false, RESTMessage.CAUSE_UNDEFINED_DATA); //  사이트에 따른 설정파일에 정의되지 않음.
                return;
            }

            // 사이트별 처리 함수를 맵으로 정의
            Map<String, Function<RESTMessage, Boolean>> handlers = Map.of(
                    Define.SITE_KBSC, this::kbsc,
                    Define.SITE_SHBK, this::shbk,
                    Define.SITE_ECS,  this::ecs
            );

            Function<RESTMessage, Boolean> handler = handlers.get(site);
            if (handler == null) {
                // 지원하지 않는 사이트
                sendResponse(msg, false, RESTMessage.CAUSE_UNDEFINED_DATA); //   정의된 함수가 없음.
            } else {
                // 실제 처리 실행
                boolean success = handler.apply(msg);   // 해당 부분 실행
                String cause   = success ? RESTMessage.CAUSE_NONE : RESTMessage.CAUSE_UNKNOWN;
                sendResponse(msg, success, cause);
            }
        } catch (Exception e) {
            UtilLog.e(getClass(), "Error in alert", e);
            sendResponse(msg, false, RESTMessage.CAUSE_UNKNOWN);
        }
    }

    @Override
    protected void onRecvMsg(Object obj) {
        if (obj instanceof RESTMessage msg) {
            alert(msg);
        }
    }

    @Override
    protected void onInit() {
    }
    @Override
    protected void onExit() {
    }
    @Override
    protected void onTimer(int i) {
    }
}
