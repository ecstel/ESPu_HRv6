package com.ecs.esp.u.hr;

import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.listener.EPropertiesListener;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.system.SystemClient;
import com.ecs.base.socket.system.SystemServer;
import com.ecs.esp.u.hr.db.HDBWorker;
import com.ecs.esp.u.hr.db.LDBWorker;
import com.ecs.esp.u.hr.db.RDBWorker;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.ConfCommon;
import com.ecs.esp.u.hr.define.Define;
import com.ecs.esp.u.hr.file.DailyBatchMonitorWorker;
import com.ecs.esp.u.hr.file.RealTimeMonitorWorker;
import com.ecs.esp.u.hr.server.RestServerWorker;
import com.ecs.esp.u.hr.tester.TesterSocket;
import com.ecs.esp.u.hr.worker.SenderRestWorker;

import static com.ecs.esp.u.com.define.ESPUtil.mapMajorToJavaVersion;

public class HRMain {
	/********************************************************************
	 * SYSTEM
	 ********************************************************************/
	private static final String APP_NAME = "HR6";
	private static final String DEF_CONF = ".\\cfg\\conf.cfg";
	private static final String DEF_CONF_COMMON = ".\\cfg\\comm.cfg";
	
	/********************************************************************
	 * LoadConfig
	 ********************************************************************/
	public static boolean LoadConfig(String path) {
		try {
			File file = new File(path);
			if(!file.exists()) {
				UtilLog.e(HRMain.class, "Config File Not Exist (Program will exit)");
				UtilLog.e(HRMain.class, "==> " + file.getAbsolutePath());
				return true;
			}
			
			if(!Conf.getInstance().Start(path, 1000, listener)) {
				UtilLog.e(HRMain.class, "Config File Load Failure (Program will exit)");
				return true;
			}
			return false;
		} catch (Exception ignored) { }
		return true;
	}
	
	/********************************************************************
	 * LoadConfigComm
	 ********************************************************************/
	public static boolean LoadConfigComm(String path) {
		try {
			File file = new File(path);
			if(!file.exists()) {
				UtilLog.e(HRMain.class, "Config Common File Not Exist (Program will exit)");
				UtilLog.e(HRMain.class, "==> " + file.getAbsolutePath());
				return true;
			}
			
			if(!ConfCommon.getInstance().Start(path, 1000, null)) {
				UtilLog.e(HRMain.class, "Config Common File Load Failure (Program will exit)");
				return true;
			}
			return false;
		} catch (Exception ignored) { }
		return true;
	}

	/********************************************************************
	 * Manifest
	 ********************************************************************/
	public static void Manifest() {
		try (InputStream is = HRMain.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
			if (is == null) {
				return;
			}
			Manifest manifest = new Manifest(is);
			Attributes attr = manifest.getMainAttributes();
			String buildTs = attr.getValue("Build-Timestamp");
			if (buildTs != null) {
				UtilLog.t(HRMain.class, "# (MANIFEST) Build Timestamp : " + buildTs);
			}
			String classPath = attr.getValue("Class-Path");
			if (classPath != null) {

				String[] parts = classPath.trim().split("\\s+");
				StringBuilder sb = new StringBuilder("\t");  // 첫 줄 시작에 \t 추가

				for (int i = 0; i < parts.length; i++) {
					sb.append(parts[i]);
					if ((i + 1) % 5 == 0) {
						sb.append("\n\t");  // 줄 바꾼 뒤 다음 줄 앞에도 \t 추가
					} else {
						sb.append(" ");
					}
				}
				UtilLog.t(HRMain.class, "# (MANIFEST) Class-Path : \n\t" + sb.toString().trim());
			}
			String jdkBuild = attr.getValue("Build-Jdk");
			if (jdkBuild != null) {
				UtilLog.t(HRMain.class, "# (MANIFEST) Build-Jdk : Java " + jdkBuild);
			}
		} catch (Exception ignored) {
		}
	}
	/********************************************************************
	 * CompiledJavaVersion
	 ********************************************************************/
	public static void CompiledJavaVersion() throws Exception {
		Class<?> clazz = HRMain.class;

		// 클래스 파일을 현재 클래스 기준으로 경로 가져오기 (리소스 경로 기준)
		String classFileName = clazz.getSimpleName() + ".class";
		try (InputStream is = clazz.getResourceAsStream(classFileName);
			 DataInputStream dis = new DataInputStream(Objects.requireNonNull(is))) {

			int magic = dis.readInt();             // 0xCAFEBABE
			int minor = dis.readUnsignedShort();   // 보통 무시
			int major = dis.readUnsignedShort();   // 주요 버전

			UtilLog.i(HRMain.class, "# Compiled Java Version : " + mapMajorToJavaVersion(major));
		}
	}
	private static final EPropertiesListener listener = new EPropertiesListener() {
		public boolean OnModify(long arg0, String arg1) {
		/*	try {

			} catch (Exception ignored) { }
		 */
			return true;
		}
	};

	/********************************************************************
	 * <p></p>
	 * Start Server Main
	 * 
	 ********************************************************************/
	public static void main(String[] args) {
		try {
			/* Argument Check */
			if(args.length <= 0) {
				if(LoadConfig(DEF_CONF)) { System.exit(0); return; }
				if(LoadConfigComm(DEF_CONF_COMMON)) { System.exit(0); return; }
			} else if(args.length == 1) {
				if(LoadConfig(args[0])) { System.exit(0); return; }
				if(LoadConfigComm(DEF_CONF_COMMON)) { System.exit(0); return; }
			} else {
				if(LoadConfig(args[0])) { System.exit(0); return; }
				if(LoadConfigComm(args[1])) { System.exit(0); return; }
			}
			if(args.length <= 3 || !args[2].equals("stop")) {
				UtilLog.i(HRMain.class, "##########################################################");
				UtilLog.i(HRMain.class, "######################### HR START #######################");
				UtilLog.i(HRMain.class, "#########################  V6.0.0  #######################");
				UtilLog.i(HRMain.class, "##########################################################");
				Manifest();
				CompiledJavaVersion();
				UtilLog.i(HRMain.class, "##########################################################");


				if(!Conf.getInstance().APP_NAME().equals(APP_NAME)) {
					UtilLog.e(HRMain.class, "Check config file [APP_NAME] Conf[" +Conf.getInstance().APP_NAME() + "] APP_NAME[" + APP_NAME + "]");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_PORT() <= 0) {
					UtilLog.e(HRMain.class, "Check config file [SYSTEM_PORT]");
					System.exit(0);
					return;
				}
				SystemServer server = new SystemServer(Conf.getInstance().SYSTEM_PORT()); 
				if(!server.Start()) {
					UtilLog.e(HRMain.class, "System Socket Start Faulure !!!");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_DELAY() > 0) {
					Thread.sleep(Conf.getInstance().SYSTEM_DELAY());
				}
				
				Conf.getInstance().TABLE_INFO();

				/* RDBWorker ( REMOTE ) */
				if(!UtilString.isEmpty(Conf.getInstance().RDB_QUERY_FILE())) {
					if(!new RDBWorker(Define.TID_RDB_WORKER, Conf.getInstance().RDB_QUERY_FILE()).Start()) {
						UtilLog.e(HRMain.class, "!!! R(Remote)DB Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! R(Remote)DB Worker Start Success !!!");
						Thread.sleep(2000);
					}
				}	
						
				/* LDBWorker ( LOCAL ) */
				if(!UtilString.isEmpty(Conf.getInstance().LDB_QUERY_FILE())) {
					if(!new LDBWorker(Define.TID_LDB_WORKER, Conf.getInstance().LDB_QUERY_FILE()).Start()) {
						UtilLog.e(HRMain.class, "!!! L(Local)DB Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! L(Local)DB Worker Start Success !!!");
						Thread.sleep(2000);
					}
				}

				/* HDBWorker ( LOCAL ) */
				if(!UtilString.isEmpty(Conf.getInstance().HDB_QUERY_FILE())) {
					if(!new HDBWorker(Define.TID_HDB_WORKER, Conf.getInstance().HDB_QUERY_FILE()).Start()) {
						UtilLog.e(HRMain.class, "!!! HDB Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! HDB Worker Start Success !!!");
						Thread.sleep(2000);
					}
				}
					
				/* RealTime File Monitor : 실시간으로 수정된 인사정보만 텍스트로 받는 경우에 대한 처리 */
				if(!UtilString.isEmpty(Conf.getInstance().REALTIME_FILE_DIR())) {
					if(Conf.getInstance().REALTIME_FILE_FIELD().contains(Define.USER_TEL)) {
						if(!new RealTimeMonitorWorker(Define.TID_REALTIME_FILE_MONITOR).Start()) {
							UtilLog.e(HRMain.class, "!!! RealTime File Monitor Worker Start Faulure !!!");
							System.exit(0);
							return;
						} else {
							UtilLog.i(HRMain.class, "!!! RealTime File Monitor Worker Start Success !!!");
						}
					} else {
						UtilLog.i(HRMain.class, "[REALTIME_FILE_FIELD] 필드에 \""+ Define.USER_TEL + "\" 항목이 존재하지 않습니다.");
					}
				}
				
				/* HRRest Worker */
				if(!UtilString.isEmpty(Conf.getInstance().PHONE_DO_URL())) {
					if(!new SenderRestWorker(Define.TID_HRREST_WORKER).Start()) {
						UtilLog.e(HRMain.class, "!!! HR Rest Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! HR Rest Start Success !!!");
					}
				}				
				
				/* DailyBatch File Monitor : 일배치 모니터링 처리 								*/
				if(!UtilString.isEmpty(Conf.getInstance().FILE_DIR())) {
					if(!new DailyBatchMonitorWorker(Define.TID_DAILYBAT_FILE_MONITOR).Start()) {
						UtilLog.e(HRMain.class, "!!! DailyBatch File Monitor Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! DailyBatch File Monitor Worker Start Success !!!");
					}
				}

				/* Start Tester */
				if(Conf.getInstance().TESTER_PORT() > 0 && UtilFile.exists(Conf.getInstance().TESTER_MAKER_FILE())) {
					if(!new TesterSocket(Define.TID_TESTER, Conf.getInstance().TESTER_PORT(), Conf.getInstance().TESTER_SAMPLE_PATH()).Start()) {
						UtilLog.e(HRMain.class, "!!! Tester Start Faulure [ " + Conf.getInstance().SITE()  + ", http://" + Conf.getInstance().SERVER_HTTP_IP() + ":" + Conf.getInstance().TESTER_PORT() + "  ] !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(HRMain.class, "!!! Tester Start Success [ " + Conf.getInstance().SITE()  + ", http://" + Conf.getInstance().SERVER_HTTP_IP() + ":" + Conf.getInstance().TESTER_PORT() + "  ] !!!");
					}
				} else {
					UtilLog.i(HRMain.class, "It does not generate test pages. port=" + Conf.getInstance().TESTER_PORT()+", makeFile=" + Conf.getInstance().TESTER_MAKER_FILE());
				}
				
				/* RestServer Server */
				if (!new RestServerWorker(Define.TID_HTTP_SERVER, Conf.getInstance().SERVER_HTTP_PORT()).Start()) {
					UtilLog.e(HRMain.class, "!!! RestServer Socket Start Faulure [" + Conf.getInstance().SERVER_HTTP_PORT() + "] !!!");
					System.exit(0);
					return;
				} else {
					UtilLog.i(HRMain.class, "!!! RestServer Socket Start Success [" + Conf.getInstance().SERVER_HTTP_PORT() + "] !!!");
				}
			
				server.waitSignal();
				System.exit(0);
			} else {
				UtilLog.i(HRMain.class, "##########################################################");
				UtilLog.i(HRMain.class, "######################### HR STOP ########################");
				UtilLog.i(HRMain.class, "#########################  V6.0.0  #######################");
				UtilLog.i(HRMain.class, "##########################################################");
				
				if(!Conf.getInstance().APP_NAME().equals(APP_NAME)) {
					UtilLog.e(HRMain.class, "Check config file [APP_NAME]");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_PORT() <= 0) {
					UtilLog.e(HRMain.class, "Check config file [SYSTEM_PORT]");
					System.exit(0);
					return;
				}
				
				SystemClient client = new SystemClient(Conf.getInstance().SYSTEM_PORT()); 
				if(!client.Connect()) {
					UtilLog.e(HRMain.class, "System Socket Start Faulure !!!");
					System.exit(0);
					return;
				}
				
				Thread.sleep(1000);
				
				client.SendStop();
				UtilLog.i(HRMain.class, "SYSTEM SEND STOP");
				
				Thread.sleep(1000);
				
				UtilLog.i(HRMain.class, "SYSTEM STOP SUCCESS");
				System.exit(0);
			}
		} catch (Exception ignored) { }
	}
}