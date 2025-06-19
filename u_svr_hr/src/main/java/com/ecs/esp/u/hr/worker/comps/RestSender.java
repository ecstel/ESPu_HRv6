package com.ecs.esp.u.hr.worker.comps;

import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.json.UtilJson;

public class RestSender {
	
	
	// 1) 클래스 단위로 공유하는 ThreadPoolExecutor 정의
    private static final ThreadPoolExecutor EXECUTOR = new ThreadPoolExecutor(
        /* corePoolSize */    Runtime.getRuntime().availableProcessors(),
        /* maximumPoolSize */ Runtime.getRuntime().availableProcessors() * 2,
        /* keepAliveTime */   60L, TimeUnit.SECONDS,
        /* workQueue */       new LinkedBlockingQueue<>(500),
        /* policy */          new ThreadPoolExecutor.CallerRunsPolicy()
    );

    // 2) 애플리케이션 종료 시 스레드 풀 정리 (예: Spring @PreDestroy 등)
    @PreDestroy
    public void shutdownExecutor() {
        EXECUTOR.shutdown();
    }

	// 3) send 메서드: 매번 풀 생성/종료 없이 EXECUTOR 재사용
    public void send(String prefix,
					 	String url,
						Map<String, String> headMap,
						Map<String, String> bodyMap,
						int connTimeOut,
                        int readTimeout)
    {
        if (UtilString.isEmpty(url)) {
            return;
        }
        CompletableFuture
            .runAsync(() -> {
                // JSON 페이로드 한 번만 인코딩
                String boydPayLoad = null;
        		try {
					boydPayLoad = UtilJson.EncodeJson(bodyMap);
        		} catch (Exception e1) {
        			e1.printStackTrace();
        		}
                String logPrefix = (prefix != null)
                    ? "[send] " + prefix + ", url = " + url + " "
                    : "[send] url = " + url + " ";
            	
                UtilLog.i(getClass(), logPrefix + boydPayLoad);
                try {
                    if (url.startsWith("https")) {
                        ClientHttpsPostJsonWithTimeout(url, boydPayLoad, connTimeOut, readTimeout);
                    } else {
                        ClientHttpPostJsonWithTimeout(url, boydPayLoad, connTimeOut, readTimeout);
                    }
                } catch (SocketTimeoutException e) {
                    UtilLog.e(getClass(),
                              "Socket timeout while sending to " + prefix, e);
                } catch (IOException e) {
                    UtilLog.e(getClass(),
                              "I/O error while sending to " + prefix, e);
                } catch (Exception e) {
                    UtilLog.e(getClass(),
                              "Unexpected error in send to " + prefix, e);
                }
            }, EXECUTOR)
            .exceptionally(ex -> {
                UtilLog.e(getClass(),
                          "CompletableFuture exception for " + prefix, ex);
                return null;
            });
    }
    
    
    /********************************************************************
	 * HTTP : ClientHttpPostJsonWithTimeout
	 ********************************************************************/
	public boolean ClientHttpPostJsonWithTimeout(String urlString, String jsonPayload, int connectTimeout, int readTimeout) throws Exception {
		URL url = new URL(urlString);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
			connection.setConnectTimeout(connectTimeout);	// 연결 타임아웃
			connection.setReadTimeout(readTimeout); 		// 읽기 타임아웃

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonPayload.getBytes("utf-8");
				os.write(input, 0, input.length);
			}

			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				UtilLog.t(getClass(), "HTTP OK response code " + responseCode);
				return true;
			} else {
				throw new IOException("HTTP request failed with response code " + responseCode);
			}
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException("Timeout occurred while connecting to the server or reading response.");
		} catch (IOException e) {
			throw new IOException("I/O error occurred while sending HTTP request or reading response.");
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
	
	/********************************************************************
	 * HTTPS : ClientHttpsPostJsonWithTimeout
	 ********************************************************************/
	public boolean ClientHttpsPostJsonWithTimeout(String urlString, String jsonPayload, int connectTimeout, int readTimeout) throws Exception {
		// Trust all certificates
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}
			public void checkClientTrusted(X509Certificate[] certs, String authType) {
			}
			public void checkServerTrusted(X509Certificate[] certs, String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc = SSLContext.getInstance("SSL");
		sc.init(null, trustAllCerts, new java.security.SecureRandom());
		HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

		// Create a HostnameVerifier that ignores verification
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};
		// Set the default HostnameVerifier
		HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);

		// Continue with the connection setup
		URL url = new URL(urlString);
		HttpsURLConnection connection = null;

		try {
			connection = (HttpsURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
			connection.setConnectTimeout(connectTimeout); 		//	Connection timeout
			connection.setReadTimeout(readTimeout); 			//	Read timeout

			try (OutputStream os = connection.getOutputStream()) {
				byte[] input = jsonPayload.getBytes("utf-8");
				os.write(input, 0, input.length);
			}
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				UtilLog.i(getClass(), "HTTPS OK response code " + responseCode);
				return true;
			} else {
				throw new IOException("HTTPS request failed with response code " + responseCode);
			}
		} catch (SocketTimeoutException e) {
			throw new SocketTimeoutException("Timeout occurred while connecting to the server or reading response.");
		} catch (IOException e) {
			throw new IOException("I/O error occurred while sending HTTPS request or reading response.");
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}
}
