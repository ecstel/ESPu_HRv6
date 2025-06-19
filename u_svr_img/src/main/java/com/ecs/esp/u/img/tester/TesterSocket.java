package com.ecs.esp.u.img.tester;

import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.socket.thread.server.EThreadServer;
import com.ecs.esp.u.com.tester.FileInformation;
import com.ecs.esp.u.com.tester.FileList;
import com.ecs.esp.u.com.tester.TesterHandler;
import com.ecs.esp.u.img.define.Conf;
import com.ecs.esp.u.img.define.ConfCommon;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

import java.io.*;
import java.nio.file.FileSystems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TesterSocket extends EThreadServer {
	/********************************************************************
	 * Static
	 ********************************************************************/
	private final List<ChannelHandlerContext> ctxList = new ArrayList<ChannelHandlerContext>();

	/********************************************************************
	 * Construct
	 ********************************************************************/
	public TesterSocket(int threadID, int port, String path) {
		super(threadID, port, Conf.getInstance().TESTER_TIMEOUT());
		TesterHandler.SetDefaultPath(path);
		TesterHandler.SetMenuFile(Conf.getInstance().TESTER_MENU_FILENAME());
		
		//	HTML Maker 파일 생성
		FileList fileListHtml = new FileList(Conf.getInstance().TESTER_MENU_FILENAME());
		fileListHtml.GetListDir(Conf.getInstance().TESTER_MAKER_PATH(), "^*html*");
		List<FileInformation> listHTML = fileListHtml.GetList();
		for( FileInformation data : listHTML ) {
		//	data.print();
			String makeFile   = String.format("%s%s%s", Conf.getInstance().TESTER_MAKER_PATH() , FileSystems.getDefault().getSeparator(), data.getFile());
			String sampleFile = String.format("%s%s%s", Conf.getInstance().TESTER_SAMPLE_PATH(), FileSystems.getDefault().getSeparator(), data.getFile());
			MakeHtmlFile(makeFile, sampleFile, Conf.getInstance().SERVER_HTTP_IP(), Conf.getInstance().SERVER_HTTP_PORT());
		}
		//	HTML Sample 파일 생성(Menu)
		FileList filelist = new FileList(Conf.getInstance().TESTER_MENU_FILENAME());
		filelist.GetListDir(Conf.getInstance().TESTER_SAMPLE_PATH(), "^*html*");
		List<FileInformation> list = filelist.GetList();
		MakeMenuFile(list, Conf.getInstance().TESTER_MAKER_FILE(), Conf.getInstance().TESTER_SAMPLE_FILE());
	}
	public final String html = """
            
            \t\t\t<tr>
            \t\t\t<th scope="row">
            \t\t\t\t<center>%s</center>
            \t\t\t</th>
            \t\t\t<td>
            \t\t\t\t<a href='./%s' target='_blank'>
            \t\t\t\t<left>%s</left>
            \t\t\t\t</a>
            \t\t\t</td>
            \t\t\t<td width="900">
            \t\t\t\t<left>%s</left>
            \t\t\t</td>
            \t\t\t<td>
            \t\t\t\t<center>%s</center>
            \t\t\t</td>
            \t\t\t</tr>
            """;

	/******************************************************************
	 * getFileTitle : 타이틀 얻기
	 ******************************************************************/
	public String getFileTitle(String filePath) {
		BufferedReader bfr = null;
		if (filePath == null || filePath.length() <= 0) {
			return "";
		}
		try {
			String title = "";
			bfr = new BufferedReader(new FileReader(new File(filePath)));
			String line;
			while ((line = bfr.readLine()) != null) {
				String data = line.trim();
				if(data.indexOf("title") > 0) {
					title = data.replaceAll("<title>", "").replaceAll("</title>", "");
					break;
				}
			}
			return title;
		} catch (Exception ignored) {
		} finally {
			if (bfr != null) {
				try {
					bfr.close();
				} catch (IOException e) {
					UtilLog.e(getClass(), e.toString());
				//	e.printStackTrace();
				}
			}
		}
		return "";
	}
	public void MakeHtmlFile(String makeFile, String sampleFile, String ip, int port) {
		try {
			byte[] msgData = UtilFile.getBytesFromFile(new File(makeFile));

            String textMsg = new String(msgData);
			textMsg = textMsg.replace("#SERVER_IP#", ip);
			textMsg = textMsg.replace("#SERVER_PORT#", String.format("%d",port));
			
			if(textMsg.contains("#DATABASE#")) {
				textMsg = textMsg.replace("#HOSTNAME#", ConfCommon.getInstance().HOSTNAME());
				textMsg = textMsg.replace("#DATABASE#", ConfCommon.getInstance().DATABASE());
				textMsg = textMsg.replace("#DBNAME#", ConfCommon.getInstance().DBNAME());
				textMsg = textMsg.replace("#DBUSER#", ConfCommon.getInstance().DBUSER());
				textMsg = textMsg.replace("#DBPORT#", ""+ConfCommon.getInstance().DBPORT());
			}
			MakeFile(sampleFile, textMsg);
		} catch (Exception ignored) { }
	}
	public void MakeMenuFile(List<FileInformation> list, String makeFile, String sampleFile) {
		byte[] msgData = UtilFile.getBytesFromFile(new File(makeFile));
		String sampleMessage = new String(msgData);
		StringBuilder htmlStr = new StringBuilder();

		list.sort(Collections.reverseOrder());
		for ( FileInformation data : list ) {
			data.print();
			String desc = getFileTitle(data.getSourceFile());
			htmlStr.append(String.format(html,
                    data.getSeq() + "",
                    data.getFile(),
                    data.getFile(),
                    desc,
                    data.getLastModifiedTime()));
		}
		String textMsg = sampleMessage;
		textMsg = textMsg.replace("#HTML_MESSAGE#", htmlStr.toString());
		MakeFile(sampleFile, textMsg);
	}
	public void MakeFile(String filePath, String strMessage) {
		try {
			File outFile = new File(filePath);	
			if(!UtilFile.exists(outFile.getParent())) {
				UtilFile.mkdirs(outFile.getParent());
			}
//			if(outFile.exists()) {
//				outFile.delete();
//			}
			FileOutputStream out = new FileOutputStream(outFile);
			out.write(strMessage.getBytes());
			out.close();
		} catch (Exception ignored) { }
	}
	/********************************************************************
	 * Handler
	 ********************************************************************/
	protected ChannelHandlerAdapter CreateDecoder() { return new TesterHandler();	}	
	protected ChannelHandlerAdapter CreateEncoder() { return null;					    	}
	protected ArrayList<ChannelHandler> CreateChannelHandler() { return DefaultFileServerHandler(this, 655360000); }
	
	/********************************************************************
	 * Netty
	 ********************************************************************/
	protected void OnRecv(ChannelHandlerContext ctx, Object obj) { }

	protected void OnAccept(ChannelHandlerContext ctx) {
		try {
			UtilLog.i(getClass(), "CONNECTED : " + ctx.channel().remoteAddress().toString());
			if(!ctxList.contains(ctx)) { ctxList.add(ctx); }
		} catch (Exception ignored) { }
	}

	protected void OnClose(ChannelHandlerContext ctx) {
		try {
			UtilLog.i(getClass(), "DISCONNECTED : " + ctx.channel().remoteAddress().toString());
			ctx.close();
            ctxList.remove(ctx);
		} catch (Exception ignored) { }
	}
	/********************************************************************
	 * EThread
	 ********************************************************************/
	protected void onRecvMsg(Object obj) { }	
	protected void onInit() { }
	protected void onExit() { }
	protected void onTimer(int arg0) { }
}