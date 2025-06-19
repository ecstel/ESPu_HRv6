package com.ecs.esp.u.alert.worker.comps;

import com.ecs.base.comm.UtilCom;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.json.UtilJson;
import com.ecs.base.socket.thread.EThread;
import com.ecs.esp.u.alert.define.Define;
import com.ecs.esp.u.alert.worker.data.SendResult;
import com.ecs.msg.rest.custom.RESTMessage;

import javax.annotation.PreDestroy;
import javax.net.ssl.*;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
					 String jobid,
					 String site,
					 String url,
					 Map<String, String> headerMap,
					 Map<String, String> bodyMap,
					 int connTimeout,
					 int readTimeout)
	{
		if (UtilString.isEmpty(url)) {
			return;
		}

		CompletableFuture
				.runAsync(() -> executeSend(prefix, jobid, site, url, headerMap, bodyMap, connTimeout, readTimeout), EXECUTOR)
				.exceptionally(ex -> {
					handleFailure(prefix, jobid, ex);
					return null;
				});
	}

	// 4. 실제 HTTP 전송 로직
	private void executeSend(String prefix,
							 String jobid,
							 String site,
							 String url,
							 Map<String, String> headerMap,
							 Map<String, String> bodyMap,
							 int connTimeout,
							 int readTimeout)
	{
		try {
			if (url.startsWith("https")) {
				httpsPost(site, url, headerMap, bodyMap, connTimeout, readTimeout);
			} else {
				httpPost(site, url, headerMap, bodyMap, connTimeout, readTimeout);
			}
			postSuccess(jobid);
		} catch (Exception e) {
			handleFailure(prefix, jobid, e);
		}
	}

	// 5. 성공 처리
	private void postSuccess(String jobid) {
		EThread.postMessage(Define.TID_DB_WORKER, new SendResult(jobid, RESTMessage.RESULT_SUCCESS, RESTMessage.CAUSE_NONE));
	}

	// 6. 실패 처리 및 로깅
	private void handleFailure(String prefix, String jobid, Throwable t) {
		String msg;
		if (t instanceof SocketTimeoutException) {
			msg = "Socket timeout while sending to " + prefix;
		} else if (t instanceof IOException) {
			msg = "I/O error while sending to " + prefix;
		} else {
			msg = "Unexpected error in send to " + prefix;
		}
		UtilLog.e(getClass(), msg, t);
		EThread.postMessage(Define.TID_DB_WORKER, new SendResult(jobid, RESTMessage.RESULT_FAIL, t.getMessage()));
	}

	/*
	 * KB증권에서 사용한 Href 처리 방식
	 */
	public String encodeHref(String input) {
		if (input == null) {
			return null;
		}
		// Regex pattern to find href attribute values
		Pattern pattern = Pattern.compile("href=\"([^\"]*)\"");
		Matcher matcher = pattern.matcher(input.replace("&", "%26"));
		StringBuilder result = new StringBuilder();

		while (matcher.find()) {
			String hrefValue = matcher.group(1);
			hrefValue = hrefValue.replace("&", "%26");
			matcher.appendReplacement(result, "href=\"" + hrefValue + "\"");
		}
		matcher.appendTail(result);
		return result.toString();
	}

	private String makeData(String site,
							HttpURLConnection connection,
							Map<String, String> headMap,
							Map<String, String> bodyMap) throws Exception
	{
		try {
			StringBuilder bodyData = new StringBuilder();
			if (Define.SITE_KBSC.equals(site)) {    //	KB증권
				if (headMap != null) {
					for (Map.Entry<String, String> entry : headMap.entrySet()) {
						connection.setRequestProperty(entry.getKey(), entry.getValue());
						UtilLog.t(getClass(), "Header => " + entry.getKey() + " : " + entry.getValue());
					}
				}
				if (bodyMap != null) {
					for (Map.Entry<String, String> entry : bodyMap.entrySet()) {
						String value = entry.getValue().replace("%", "%25");    //	%를 %25로 변경함
						bodyData.append(entry.getKey()).append("=").append(encodeHref(value));
						UtilLog.t(getClass(), "Body => " + entry.getKey() + " : " + entry.getValue() + " : " + value);
						bodyData.append("&");
					}
					bodyData = new StringBuilder(UtilCom.removeTrailingAmpersand(bodyData.toString()));
				}
			} else {    //	그외
				if (headMap != null) {
					for (Map.Entry<String, String> entry : headMap.entrySet()) {
						connection.setRequestProperty(entry.getKey(), entry.getValue());
						UtilLog.t(getClass(), "Header => " + entry.getKey() + " : " + entry.getValue());
					}
				}
				bodyData = new StringBuilder(UtilJson.EncodeJson(bodyMap));
				UtilLog.t(getClass(), "bodyData => " + bodyData);
			}
			return bodyData.toString();
		} catch (Exception e) {
			throw new Exception("파일을 찾을 수 없습니다: ");
		}
	}

	/********************************************************************
	 * httpPost
	 ********************************************************************/
	public void httpPost(
					String site,
					String urlString,
					Map<String, String> headerMap,
					Map<String, String> bodyMap,
					int connectTimeout,
					int readTimeout) throws Exception
	{
		URL url = new URL(urlString);
		HttpURLConnection connection = null;
		try {
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/json; utf-8");
			connection.setRequestProperty("Accept", "application/json");
			connection.setDoOutput(true);
			connection.setConnectTimeout(connectTimeout);    	// Connection timeout
			connection.setReadTimeout(readTimeout);        		// Read timeout
			String bodyData = makeData(site, connection, headerMap, bodyMap);

			UtilLog.i(getClass(), "{httpPost} Send site=" + site +
					", url=" + url + ", header=" + headerMap + ", body=" + bodyData);

			byte[] input = bodyData.getBytes(StandardCharsets.UTF_8 );
			connection.setFixedLengthStreamingMode(input.length);
			try (OutputStream os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
			}
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// ▶ 성공 시 InputStream 열기 → 닫기
				try (InputStream is = connection.getInputStream();
					 BufferedReader reader = new BufferedReader(
							 new InputStreamReader(is, StandardCharsets.UTF_8))) {
					// 필요하면 실제로 읽어서 로그에 남기거나 무시할 수 있습니다.
					String line;
					StringBuilder sb = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					UtilLog.i(getClass(), "{httpPost} Recv " +  sb.substring(0, Math.min(sb.length(), 512)));
				}
			} else {
				// ▶ 실패 시 ErrorStream 열기 → 닫기
				InputStream es = connection.getErrorStream();
				if (es != null) {
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(es, StandardCharsets.UTF_8))) {
						String line;
						StringBuilder sb = new StringBuilder();
						while ((line = reader.readLine()) != null) {
							sb.append(line);
						}
						UtilLog.i(getClass(), "{httpPost} Recv " + sb.substring(0, Math.min(sb.length(), 512)));
					}
				}
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
	 * httpsPost
	 ********************************************************************/
	public void httpsPost(
			String site,
			String urlString,
			Map<String, String> headerMap,
			Map<String, String> bodyMap,
			int connectTimeout,
			int readTimeout) throws Exception
	{
		// Trust all certificates
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public X509Certificate[] getAcceptedIssuers() {
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
			String bodyData = makeData(site, connection, headerMap, bodyMap);

			UtilLog.i(getClass(), "{httpsPost} Send site=" + site +
					", url=" + url + ", header=" + headerMap + ", body=" + bodyData);

			byte[] input = bodyData.getBytes(StandardCharsets.UTF_8 );
			connection.setFixedLengthStreamingMode(input.length);
			try (OutputStream os = connection.getOutputStream()) {
				os.write(input, 0, input.length);
			}
			int responseCode = connection.getResponseCode();
			if (responseCode == HttpURLConnection.HTTP_OK) {
				// ▶ 성공 시 InputStream 열기 → 닫기
				try (InputStream is = connection.getInputStream();
					 BufferedReader reader = new BufferedReader(
							 new InputStreamReader(is, StandardCharsets.UTF_8))) {
					// 필요하면 실제로 읽어서 로그에 남기거나 무시할 수 있습니다.
					String line;
					StringBuilder sb = new StringBuilder();
					while ((line = reader.readLine()) != null) {
						sb.append(line);
					}
					UtilLog.i(getClass(), "{httpsPost} Recv " + sb.substring(0, Math.min(sb.length(), 512)));
				}
			} else {
				// ▶ 실패 시 ErrorStream 열기 → 닫기
				InputStream es = connection.getErrorStream();
				if (es != null) {
					try (BufferedReader reader = new BufferedReader(
							new InputStreamReader(es, StandardCharsets.UTF_8))) {
						String line;
						StringBuilder sb = new StringBuilder();
						while ((line = reader.readLine()) != null) {
							sb.append(line);
						}
						UtilLog.e(getClass(), "{httpsPost} Recv " + sb.substring(0, Math.min(sb.length(), 512)));
					}
				}
				throw new IOException("HTTPs request failed with response code " + responseCode);
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