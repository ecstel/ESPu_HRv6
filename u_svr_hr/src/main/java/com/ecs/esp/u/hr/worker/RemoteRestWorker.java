package com.ecs.esp.u.hr.worker;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.proc.EThreadWorker;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.esp.u.hr.worker.data.shbk.SHBKResponse;
import com.ecs.esp.u.hr.worker.data.shbk.SHBKUser;
import com.ecs.msg.rest.custom.RESTMessage;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

public class RemoteRestWorker extends EThreadWorker {


    /********************************************************************
     * Constructor
     ********************************************************************/
    public RemoteRestWorker(int threadID) {
        super(threadID);
    }

    /****************************************************************************************
     *
     ****************************************************************************************/
    protected String ivrSync(RESTMessage msg) {
        String urlAddr = "http://112.168.34.235:8001/inf/ivr/svc_code/list";
        HttpURLConnection connection = null;
        try {
            // 1) 연결 설정
            URL url = new URL(urlAddr);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Accept", "application/json");

            // 2) 응답 코드 확인
            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                // 3) 한 줄만 읽기
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    String responseLine = br.readLine();  // 첫 줄만 읽음
                    if (responseLine != null) {
                        UtilLog.i(getClass(), "Response: " + responseLine);
                        return responseLine;
                    }
                }
            } else {
                UtilLog.i(getClass(), "GET request failed, Response Code: " + responseCode);
            }
        } catch (Exception e) {
            UtilLog.e(getClass(), e);
            e.printStackTrace();
        } finally {
            // 4) 바로 연결 끊기
            if (connection != null) {
                connection.disconnect();
            }
        }
        return null;
    }


    protected void test(RESTMessage msg) {

        String jsonData  = ivrSync(msg);


        if(!UtilString.isEmpty(jsonData)) {

            // ② ObjectMapper 설정
            ObjectMapper mapper = new ObjectMapper()
                    // 모르는 필드가 있어도 예외 던지지 않도록
                    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

            try {
                // ③ JSON → SvcResponse 객체 변환
                SHBKResponse resp = mapper.readValue(jsonData, SHBKResponse.class);

                // ④ 결과 사용
                System.out.println("code: " + resp.getCode());
                System.out.println("message: " + resp.getMessage());
                System.out.println("status: " + resp.getStatus());


                int i = 1;
                List<SHBKUser> list = resp.getUsers();
                int total = list.size();
                for (SHBKUser user : list) {
                    UtilLog.i(getClass(), String.format("[%04d/%04d] => %s ", i, total, user.toString()));
                    i++;
                }
            //  insert(list);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //  TODO 채워넣음

    protected void rest(RESTMessage msg) {
        try {
            String site = Optional.ofNullable(msg.GetParamJson(RESTMessage.CODE_SITE))
                    .map(String::toUpperCase)
                    .orElse("");

            if (site.isEmpty()) {
                UtilLog.e(getClass(), "SITE(" + site + ") 필드 값이 존재하지 않습니다.");
                return;
            }
            if(!Conf.getInstance().SITE().equalsIgnoreCase(site)) {
                UtilLog.e(getClass(), " SITE(" + site + ") 와 환경 설정값("+Conf.getInstance().SITE()+")이 서로 다릅니다.");
                return;
            }

            // 사이트별 처리 함수를 맵으로 정의
            Map<String, Function<RESTMessage, Boolean>> handlers = Map.of(
                    Define.SITE_SHBK, this::shbk
            );

            Function<RESTMessage, Boolean> handler = handlers.get(site);
            if (handler == null) {
                // 지원하지 않는 사이트
            //    sendResponse(msg, false, RESTMessage.CAUSE_UNDEFINED_DATA); //   정의된 함수가 없음.
            } else {
                // 실제 처리 실행
                boolean success = handler.apply(msg);   // 해당 부분 실행
                String cause   = success ? RESTMessage.CAUSE_NONE : RESTMessage.CAUSE_UNKNOWN;
            //  sendResponse(msg, success, cause);
            }
        } catch (Exception e) {
            UtilLog.e(getClass(), "Error in alert", e);
        //  sendResponse(msg, false, RESTMessage.CAUSE_UNKNOWN);
        }
    }

    private Boolean shbk(RESTMessage restMessage) {
    //  TODO 채워넣음

        return true;
    }


    @Override
    protected void onRecvMsg(Object o) {
        if(o instanceof RESTMessage msg) {
            if (msg.getMessageID() == RESTMessage.REQ_REST_SYNC_BATCH) {
                rest(msg);
            }
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
