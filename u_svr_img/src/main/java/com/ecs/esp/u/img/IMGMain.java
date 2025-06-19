package com.ecs.esp.u.img;

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
import com.ecs.esp.u.img.db.DBWorker;
import com.ecs.esp.u.img.define.Conf;
import com.ecs.esp.u.img.define.ConfCommon;
import com.ecs.esp.u.img.define.Define;
import com.ecs.esp.u.img.server.RestServerWorker;
import com.ecs.esp.u.img.tester.TesterSocket;

import static com.ecs.esp.u.com.define.ESPUtil.mapMajorToJavaVersion;

public class IMGMain {
	/********************************************************************
	 * SYSTEM
	 ********************************************************************/
	private static final String APP_NAME = "IMG6";
	private static final String DEF_CONF = ".\\cfg\\conf.cfg";
	private static final String DEF_CONF_COMMON = ".\\cfg\\comm.cfg";

	/********************************************************************
	 * LoadConfig
	 ********************************************************************/
	public static boolean LoadConfig(String path) {
		try {
			File file = new File(path);
			if(!file.exists()) {
				UtilLog.e(IMGMain.class, "Config File Not Exist (Program will exit)");
				UtilLog.e(IMGMain.class, "==> " + file.getAbsolutePath());
				return true;
			}

			if(!Conf.getInstance().Start(path, 1000, listener)) {
				UtilLog.e(IMGMain.class, "Config File Load Failure (Program will exit)");
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
				UtilLog.e(IMGMain.class, "Config Common File Not Exist (Program will exit)");
				UtilLog.e(IMGMain.class, "==> " + file.getAbsolutePath());
				return true;
			}

			if(!ConfCommon.getInstance().Start(path, 1000, null)) {
				UtilLog.e(IMGMain.class, "Config Common File Load Failure (Program will exit)");
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
		try (InputStream is = IMGMain.class.getClassLoader().getResourceAsStream("META-INF/MANIFEST.MF")) {
			if (is == null) {
				return;
			}
			Manifest manifest = new Manifest(is);
			Attributes attr = manifest.getMainAttributes();
			String buildTs = attr.getValue("Build-Timestamp");
			if (buildTs != null) {
				UtilLog.t(IMGMain.class, "# (MANIFEST) Build Timestamp : " + buildTs);
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
				UtilLog.t(IMGMain.class, "# (MANIFEST) Class-Path : \n\t" + sb.toString().trim());
			}
			String jdkBuild = attr.getValue("Build-Jdk");
			if (jdkBuild != null) {
				UtilLog.t(IMGMain.class, "# (MANIFEST) Build-Jdk : Java " + jdkBuild);
			}
		} catch (Exception ignored) {
		}
	}
	/********************************************************************
	 * CompiledJavaVersion
	 ********************************************************************/
	public static void CompiledJavaVersion() throws Exception {
		Class<?> clazz = IMGMain.class;

		// 클래스 파일을 현재 클래스 기준으로 경로 가져오기 (리소스 경로 기준)
		String classFileName = clazz.getSimpleName() + ".class";
		try (InputStream is = clazz.getResourceAsStream(classFileName);
			 DataInputStream dis = new DataInputStream(Objects.requireNonNull(is))) {

			int magic = dis.readInt();             // 0xCAFEBABE
			int minor = dis.readUnsignedShort();   // 보통 무시
			int major = dis.readUnsignedShort();   // 주요 버전

			UtilLog.i(IMGMain.class, "# Compiled Java Version : " + mapMajorToJavaVersion(major));
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
				UtilLog.i(IMGMain.class, "##########################################################");
				UtilLog.i(IMGMain.class, "######################### IMG START ######################");
				UtilLog.i(IMGMain.class, "#########################  V6.0.0  #######################");
				UtilLog.i(IMGMain.class, "##########################################################");
				Manifest();
				CompiledJavaVersion();
				UtilLog.i(IMGMain.class, "##########################################################");

				if(!Conf.getInstance().APP_NAME().equals(APP_NAME)) {
					UtilLog.e(IMGMain.class, "Check config file [APP_NAME] Conf[" +Conf.getInstance().APP_NAME() + "] APP_NAME[" + APP_NAME + "]");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_PORT() <= 0) {
					UtilLog.e(IMGMain.class, "Check config file [SYSTEM_PORT]");
					System.exit(0);
					return;
				}
				SystemServer server = new SystemServer(Conf.getInstance().SYSTEM_PORT());
				if(!server.Start()) {
					UtilLog.e(IMGMain.class, "System Socket Start Faulure !!!");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_DELAY() > 0) {
					Thread.sleep(Conf.getInstance().SYSTEM_DELAY());
				}
				Conf.getInstance().LoadCfgImage();

				/* DBWorker */
				if(!UtilString.isEmpty(Conf.getInstance().QUERY_FILE())) {
					if(!new DBWorker(Define.TID_DB_WORKER, Conf.getInstance().QUERY_FILE()).Start()) {
						UtilLog.e(IMGMain.class, "!!! DB Worker Start Faulure !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(IMGMain.class, "!!! DB Worker Start Success !!!");
					}
				}

				/* Start Tester */
				if(Conf.getInstance().TESTER_PORT() > 0 && UtilFile.exists(Conf.getInstance().TESTER_MAKER_FILE())) {
					if(!new TesterSocket(Define.TID_TESTER, Conf.getInstance().TESTER_PORT(), Conf.getInstance().TESTER_SAMPLE_PATH()).Start()) {
						UtilLog.e(IMGMain.class, "!!! Tester Start Faulure [" + Conf.getInstance().TESTER_PORT() + "] !!!");
						System.exit(0);
						return;
					} else {
						UtilLog.i(IMGMain.class, "!!! Tester Start Success [  http://" + Conf.getInstance().SERVER_HTTP_IP() + ":" + Conf.getInstance().TESTER_PORT() + "  ] !!!");
					}
				} else {
					UtilLog.i(IMGMain.class, "It does not generate test pages. port=" + Conf.getInstance().TESTER_PORT()+", makeFile=" + Conf.getInstance().TESTER_MAKER_FILE());
				}

				/* RestServer Server */
				if (!new RestServerWorker(Define.TID_HTTP_SERVER, Conf.getInstance().SERVER_HTTP_PORT()).Start()) {
					UtilLog.e(IMGMain.class, "!!! RestServer Socket Start Faulure [" + Conf.getInstance().SERVER_HTTP_PORT() + "] !!!");
					System.exit(0);
					return;
				} else {
					UtilLog.i(IMGMain.class, "!!! RestServer Socket Start Success [" + Conf.getInstance().SERVER_HTTP_PORT() + "] !!!");
				}

				server.waitSignal();
				System.exit(0);
			} else {
				UtilLog.i(IMGMain.class, "##########################################################");
				UtilLog.i(IMGMain.class, "######################### IMG STOP #######################");
				UtilLog.i(IMGMain.class, "#########################  V6.0.0  #######################");
				UtilLog.i(IMGMain.class, "##########################################################");

				if(!Conf.getInstance().APP_NAME().equals(APP_NAME)) {
					UtilLog.e(IMGMain.class, "Check config file [APP_NAME]");
					System.exit(0);
					return;
				}
				if(Conf.getInstance().SYSTEM_PORT() <= 0) {
					UtilLog.e(IMGMain.class, "Check config file [SYSTEM_PORT]");
					System.exit(0);
					return;
				}

				SystemClient client = new SystemClient(Conf.getInstance().SYSTEM_PORT());
				if(!client.Connect()) {
					UtilLog.e(IMGMain.class, "System Socket Start Faulure !!!");
					System.exit(0);
					return;
				}

				Thread.sleep(1000);

				client.SendStop();
				UtilLog.i(IMGMain.class, "SYSTEM SEND STOP");

				Thread.sleep(1000);

				UtilLog.i(IMGMain.class, "SYSTEM STOP SUCCESS");
				System.exit(0);
			}
		} catch (Exception ignored) { }
	}
}