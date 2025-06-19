package com.ecs.esp.u.img.db;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.FileImageOutputStream;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.EDBPostgres;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.http.client.UtilHttpClient;
import com.ecs.base.socket.thread.proc.EThreadWorkerPoolDB2;
import com.ecs.esp.u.com.bulk.Bulk;
import com.ecs.esp.u.com.bulk.BulkResult;
import com.ecs.esp.u.com.bulk.DataBulk;
import com.ecs.esp.u.com.define.CommonConstants;
import com.ecs.esp.u.img.db.data.CfgImage;
import com.ecs.esp.u.img.define.Conf;
import com.ecs.esp.u.img.define.ConfCommon;
import com.ecs.esp.u.img.define.Define;
import com.ecs.msg.rest.custom.RESTMessage;

/******************************************************
 * Local DB 처리
 * 
 * @author khkwon
 *
 ******************************************************/
public class DBWorker extends EThreadWorkerPoolDB2 {
	/****************************************************************
	 * Data
	 ****************************************************************/
	private final String 		queryFile;
	private DataSource 	dbData;

	/****************************************************************
	 * Constructor
	 ****************************************************************/
	public DBWorker(int threadID, String queryFile) {
		super(threadID);
		this.queryFile = queryFile;
	}

	/********************************************************************
	 * Start
	 ********************************************************************/
	public boolean Start() {
		try {
			EDBM dbm = DBConnect();
			if (dbm != null) {
				dbData = new DataSource(this.queryFile);
				if(dbData.Init(dbm)){
					UtilLog.i(getClass(), "Start Init.");
				}
				UtilLog.i(getClass(), "[Start] " + dbm.GetConnString());
			}
			return super.Start(Conf.getInstance().DB_WORKER_ETHREADS());
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return false;
	}

	/********************************************************************
	 * Stop
	 ********************************************************************/
	public boolean Stop() {
		super.stop();
		super.DBClose();
		return true;
	}

	/********************************************************************
	 * DBConnect
	 ********************************************************************/
	private EDBM DBConnect() {
		try {
			super.Config(this.queryFile, ConfCommon.getInstance());
			EDBM dbm = GetDBConnect();
			if (dbm == null) {
				dbm = CreateDBConnect();
				SetDBConnect(dbm);
				UtilLog.t(getClass(), "DATABASE CONNECTED !!!");
			}
			if (DBConnect(dbm)) {
				return dbm;
			}
			UtilLog.e(getClass(), "DATABASE NOT CONNECTED !!!");
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return null;
	}
	
	/**********************************************************************************************************
	 * CreateTable : 기본 테이블 자동 생성
	 * @param dbm EDBM 객체
	 * @param tableName 테이블명
	 * @param cellList 필드리스트
	 * @return
	 **********************************************************************************************************/
	protected boolean createTable(EDBM dbm, String tableName, List<String> cellList) {
		try {
			String createHead = "";
			String createBody = "";
			String createTime = "";
			String createEnd  = "";
			if(dbm instanceof EDBPostgres) {
				createHead = "CREATE TABLE %s ( ";
				createBody = "%s VARCHAR, ";
				createTime = "UPDATETIME VARCHAR";	
				createEnd  = ")";		
			} else {
				createHead = "CREATE TABLE %s ( ";
				createBody = "%s VARCHAR(512), ";
				createTime = "UPDATETIME VARCHAR(64) ";
				createEnd  = ")";		
			}
			String body = "";
			for ( String data : cellList) {
				body += String.format(createBody, data);
				if(UtilString.isEmpty(data)) {
					return false;
				}
			}
			String sql = String.format(createHead, tableName);
			sql += body;	//	UtilCom.replaceLast(body, ",", "");
			sql += createTime;
			sql += createEnd;
			dbData.CreateTable(dbm, tableName, sql);		
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}

	/**********************************************************************************************************
	 * dbBulk : Bulk, Batch 동시 처리
	 * @param data
	 * @return
	 * @throws Exception
	 **********************************************************************************************************/
	protected boolean dbBulk(DataBulk data) throws Exception {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return false;
		}
		try {
			String sql = null;
			dbData.SetTransaction(dbm, true);
			if (data.isDailyBatch()) {
				if (dbData.IsQuery(dbm, CommonConstants.FD_INSERT_BATCH)) {
					UtilLog.i(getClass(), " 일배치(BATCH), 항목=" + data.title);
					return dbData.Update(dbm, CommonConstants.FD_INSERT_BATCH, data);
				} else {
					if(dbm instanceof EDBPostgres) {
						data.cmplt_dt = UtilCalendar.getLongToString("yyyyMMddHHmmssSSS");
						sql = String.format(Bulk.SQLBatchv2, data.req_url, "SYSTEM", "COMPLETED", data.title, data.description, data.result, data.cause);
					}
					if(!UtilString.isEmpty(sql)) {
						UtilLog.i(getClass(), " 일배치(SQLBatchv2), 항목=" + data.title);
						return dbData.Execute(dbm, sql);
					} else {
						UtilLog.e(getClass(), " 일배치, 항목=" + data.title + " 질의 정보가 없습니다.");
					}
				}
			} else {
				if (dbData.IsQuery(dbm, CommonConstants.FD_UPDATE_BULK)) {
					UtilLog.i(getClass(), " 실시간(BULK), 항목=" + data.title);
					return dbData.Update(dbm, CommonConstants.FD_UPDATE_BULK, data);
				} else {
					if(dbm instanceof EDBPostgres) {
						data.cmplt_dt = UtilCalendar.getLongToString("yyyyMMddHHmmssSSS");
						sql = String.format(Bulk.SQLBulkv2, data.title, data.description, data.file_nm, data.result, data.cause, data.token);
					}
					if(!UtilString.isEmpty(sql)) {
						UtilLog.i(getClass(), " 실시간(SQLBulkv2), 항목=" + data.title);
						return dbData.Execute(dbm, sql);
					} else {
						UtilLog.e(getClass(), " 실시간, 항목=" + data.title + " 질의 정보가 없습니다.");
					}
				}
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), "ROLLBACK{dbBulk} " + e.getMessage());
			dbData.Rollback(dbm);
		} finally {
			UtilLog.i(getClass(), "SetTransaction{dbBulk} 처리 완료");
			dbData.SetTransaction(dbm, false);
		}
		return false;
	}
	
	/**********************************************************************************************************
	 * 
	 * @param strUrl
	 * @param destPath
	 * @param connTimeOut
	 * @param readTimeOut
	 * @return
	 **********************************************************************************************************/
	public boolean downloadFile(String strUrl, String destPath, int connTimeOut, int readTimeOut) {
		FileOutputStream fos = null;
		InputStream is = null;
		try {
			// Trust all certificates (not recommended for production)
			TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
				public java.security.cert.X509Certificate[] getAcceptedIssuers() {
					return null;
				}

				public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}

				public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
				}
			} };

			// Activate the new trust manager
			try {
				SSLContext sc = SSLContext.getInstance("SSL");
				sc.init(null, trustAllCerts, new java.security.SecureRandom());
				HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
			} catch (Exception e) {
				e.printStackTrace();
				return false;
			}

			// Set URL and establish connection
			URL u = new URL(strUrl);
			HttpsURLConnection connection = (HttpsURLConnection) u.openConnection();

			// Set timeouts
			connection.setConnectTimeout(connTimeOut); // 5 seconds to establish connection
			connection.setReadTimeout(readTimeOut); // 5 seconds to read data

			// Connect to the server
			connection.connect();

			// Ensure the directory exists
			File filePath = new File(destPath);
			File fileDir = filePath.getParentFile();
			if (fileDir != null) {
				fileDir.mkdirs();
			}

			fos = new FileOutputStream(filePath);
			is = connection.getInputStream();

			// Read data and write to the file
			byte[] buf = new byte[102400];
			int len;
			while ((len = is.read(buf)) > 0) {
				fos.write(buf, 0, len);
			}

			fos.close();
			is.close();

			return filePath.exists();
		} catch (Exception e) {
		//	e.printStackTrace();
		//	UtilLog.e(getClass(), "{FileDownload} " + e.getMessage());
			try {
				if (fos != null) {
					fos.close();
				}
				if (is != null) {
					is.close();
				}
			} catch (Exception e1) {
			//	e1.printStackTrace();
				UtilLog.e(getClass(), "{FileDownload} " + e1.getMessage());
			}
			// Delete the file if it was partially downloaded
			File file = new File(destPath);
			if (file.exists() && file.length() <= 0) {
				file.delete();
			}
		}
		return false;
	}
	
	/********************************************************************
	 * getUrlFile : url 경로의 필드 변경
	 * @param imgURL
	 * @param userid
	 * @param picture
	 * @param picurl
	 * @return
	 ********************************************************************/
	public String getUrlFile(String imgURL, String userid, String picture, String picurl) {
		String urlFile    = "";
		if(imgURL.contains("#")) {
			urlFile = imgURL.replace("#user_id#",  	userid);
			urlFile = urlFile.replace("#picture#", 	picture);
			urlFile = urlFile.replace("#picurl#",  	picurl);
		} else {
			urlFile = imgURL.replaceAll("%s", 		userid);
		}
		return urlFile;
	}

	/********************************************************************
	 * downLoadImage
	 * @param userid
	 * @param picture
	 * @param picurl
	 * @param img
	 ********************************************************************/
	public boolean downLoadImage(String userid, String picture, String picurl, CfgImage img) {
		String imgURL  = img.getUrl();
		String downDir = Conf.getInstance().DOWNLOAD_DIRECTORY();
		String downTmp = Conf.getInstance().DOWNLOAD_DIRECTORY()+File.separator+"pic";
		//	디렉토리 생성
		if(!UtilFile.exists(downDir)) {
			UtilFile.mkdirs(downDir);
			UtilFile.mkdirs(downTmp);
		}
		String imageFile = "";
		boolean isSuccess = false;
		try {
			String urlFile    = getUrlFile(imgURL, userid, picture, picurl);
			String tempFile   = String.format("%s%s%s.%s", downTmp, File.separator, userid, "jpg");		//	임시 저장
			String orgiFile   = String.format("%s%s%s.%s", downDir, File.separator, userid, "jpg");		//	실제 저장
			boolean isFile    = false;
			if(urlFile.startsWith("https://")) {
				isFile = downloadFile(urlFile, tempFile, 5000, 5000);
			} else {
				isFile = UtilHttpClient.FileDownload(urlFile, tempFile);
			}
			if(isFile) {
				if(Conf.getInstance().PIC_WIDTH() > 0) {
					//	1. 프레임 생성
					Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
					ImageWriter writer = (ImageWriter)iter.next();
				
					ImageWriteParam iwp = writer.getDefaultWriteParam();
					iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
					iwp.setCompressionQuality(1);   
		
					BufferedImage mcid = new BufferedImage(Conf.getInstance().PIC_WIDTH(), Conf.getInstance().PIC_HEIGHT(), BufferedImage.TYPE_INT_RGB);
					Graphics2D g2d 			   = mcid.createGraphics();
		
					RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
					rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
					g2d.setRenderingHints(rh);		        
					g2d.setColor(Color.WHITE);
					
					//	2. 배경이미지 그려주고, 이미지 크기 조절...
					imageFile 		= tempFile;
					BufferedImage backImage = ImageIO.read(new File(imageFile));
					g2d.drawImage(backImage,  0, 0, Conf.getInstance().PIC_WIDTH(), Conf.getInstance().PIC_HEIGHT(), null); 
					g2d.dispose();
					
				    String resultFile      = orgiFile;
					File outputfile = new File(resultFile);
					FileImageOutputStream output = new FileImageOutputStream(outputfile);
					writer.setOutput(output);
					IIOImage image = new IIOImage(mcid, null, null);
					writer.write(null, image, iwp);
					writer.dispose();
					output.close();
					
					if (Conf.getInstance().FILE_SIZE() > 0) {
						long lsize = UtilFile.sizeOfFile(new File(tempFile));
						if(lsize < Conf.getInstance().FILE_SIZE()) {
							UtilLog.e(getClass(), " REMOVE FILE((R) : " + resultFile + " 파일 사이즈가 너무 작습니다.(" + lsize + ")");
							UtilFile.remove(resultFile);
						} else {
							UtilLog.i(getClass(), " FILE SUCCESS(R) : " + resultFile);
							isSuccess = true;
						}
					} else {
						UtilLog.i(getClass(), " FILE SUCCESS(R) : " + resultFile);
						isSuccess = true;
					}
					UtilFile.remove(tempFile);
				} else {
					UtilFile.copyFile(tempFile, orgiFile);
					UtilFile.remove(tempFile);
					if (Conf.getInstance().FILE_SIZE() > 0) {
						long lsize = UtilFile.sizeOfFile(new File(orgiFile));
						if(lsize < Conf.getInstance().FILE_SIZE()) {
							UtilLog.e(getClass(), " REMOVE FILE(O) : " + orgiFile + " 파일 사이즈가 너무 작습니다.(" + lsize + ")");
							UtilFile.remove(orgiFile);
						} else {
							UtilLog.i(getClass(), " FILE SUCCESS(O) : " + orgiFile);
							isSuccess = true;
						}
					} else {
						UtilLog.i(getClass(), " FILE SUCCESS(O) : " + orgiFile);
						isSuccess = true;
					}
				}
			} else {
				UtilLog.i(getClass(), " URL  FAILED     : " + urlFile);
				UtilFile.remove(tempFile);
			}
		} catch(Exception e) { UtilLog.e(getClass(), "imageFile "+ imageFile + " \n" + e); }
		return isSuccess;
	}

	/********************************************************************
	 * updatePhotoInfoInDb
	 ********************************************************************/
	protected boolean updatePhotoInfoInDb(List<DataDB2> list) {
		EDBM dbm = DBConnect();
		if (dbm == null) {
			return false;
		}
		try {
			dbm.SetTransaction(true);				
			if(dbData.IsQuery(dbm, CommonConstants.FD_CLEAR)) {
				dbData.Delete(dbm, CommonConstants.FD_CLEAR, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_UPDATE)) {
				UtilLog.i(getClass(), " 사진정보 DB 갱신 ");
				return dbData.Update(dbm, CommonConstants.FD_UPDATE, list);
			}
		} catch(Exception e) {
			dbm.Rollback();
			UtilLog.e(getClass(), e);
		} finally {
			dbm.SetTransaction(false);
		}
		return false;
	}
	
	
	/**********************************************************************************
	 * resizeCopyFile thumbnail 사진 사이즈 조절
	 * @param orgiFile
	 * @param targeFile
	 * @return
	 **********************************************************************************/
	public boolean resizeCopyFile(String orgiFile, String targeFile) {
		try {
			if(Conf.getInstance().THUMBNAIL_WIDTH() > 0) {
				//	1. 프레임 생성
				Iterator<ImageWriter> iter = ImageIO.getImageWritersByFormatName("jpeg");
				ImageWriter writer = (ImageWriter)iter.next();
			
				ImageWriteParam iwp = writer.getDefaultWriteParam();
				iwp.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
				iwp.setCompressionQuality(1);   
	
				BufferedImage mcid = new BufferedImage(Conf.getInstance().THUMBNAIL_WIDTH(), Conf.getInstance().THUMBNAIL_HEIGHT(), BufferedImage.TYPE_INT_RGB);
				Graphics2D g2d 			   = mcid.createGraphics();
	
				RenderingHints rh = new RenderingHints(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
				rh.put(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
				g2d.setRenderingHints(rh);		        
				g2d.setColor(Color.WHITE);
				
				//	2. 배경이미지 그려주고, 이미지 크기 조절...
				BufferedImage backImage = ImageIO.read(new File(orgiFile));
				g2d.drawImage(backImage,  0, 0, Conf.getInstance().THUMBNAIL_WIDTH(), Conf.getInstance().THUMBNAIL_HEIGHT(), null); 
				g2d.dispose();
				
				File outputfile = new File(targeFile);
				FileImageOutputStream output = new FileImageOutputStream(outputfile);
				writer.setOutput(output);
				IIOImage image = new IIOImage(mcid, null, null);
				writer.write(null, image, iwp);
				writer.dispose();
				output.close();
				return true;
			} else {
				UtilFile.copyFile(orgiFile, targeFile); 
				return true;
			}
		} catch(Exception e) { UtilLog.e(getClass(), "orgiFile "+ orgiFile + "targeFile = " + targeFile + " \n" + e); }

		return false;
	}

	/**********************************************************************************
	 * thumbNail 
	 * @param picture
	 * @return
	 **********************************************************************************/
	protected boolean thumbNail(String picture) {
		try {
			if (!UtilString.isEmpty(Conf.getInstance().THUMBNAIL_DIRECTORY())) {
				UtilFile.mkdirs(Conf.getInstance().THUMBNAIL_DIRECTORY());
				String orgiFile 	= String.format("%s%s%s", 	Conf.getInstance().DOWNLOAD_DIRECTORY(),  File.separator, picture); 	// 실제 저장
				String targeFile    = String.format("%s%s%s", 	Conf.getInstance().THUMBNAIL_DIRECTORY(), File.separator, picture); 	// 복제 경로의 사진 URL
				if (!UtilString.isEmpty(Conf.getInstance().THUMBNAIL_DEFAULT())) { 				// 기본값 사용으로 되면 모두 기본값으로 사용함.
					resizeCopyFile(Conf.getInstance().THUMBNAIL_DEFAULT(), targeFile);
					UtilLog.t(getClass(), "{thumbNail} Copy1 : " + Conf.getInstance().THUMBNAIL_DEFAULT() + " => " + targeFile);
					UtilLog.t(getClass(), "\tTHUMBNAIL-1 : " + targeFile);
				} else {
					if (UtilFile.isFile(orgiFile)) { 			// 인사에 사진이 존재하면
						resizeCopyFile(orgiFile, targeFile);
						UtilLog.t(getClass(), "{thumbNail} Copy2 : " + orgiFile + " => " + targeFile);
						UtilLog.t(getClass(), "\tTHUMBNAIL-2 : " + targeFile);
					} else {
						if (!UtilString.isEmpty(Conf.getInstance().THUMBNAIL_EMPTY())) {
							resizeCopyFile(Conf.getInstance().THUMBNAIL_EMPTY(), targeFile);
							UtilLog.t(getClass(), "{thumbNail} Copy3 : " + Conf.getInstance().THUMBNAIL_EMPTY() + " => " + targeFile);
							UtilLog.t(getClass(), "\tTHUMBNAIL-3 : " + targeFile);
						}
					}
				}
				return true;
			}
		} catch(Exception e) {}
		return false;
	}
	/********************************************************************
	 * syncPicture
	 ********************************************************************/
	protected BulkResult syncPicture(RESTMessage msg) {
		BulkResult result = new BulkResult();
		EDBM dbm = DBConnect();
		if (dbm == null) {
			result.setCause(BulkResult.BULK_DB_CONNECTION_FAIL);
			return result;
		}
		try {
			
			UtilLog.i(getClass(), " ####################### ");
			UtilLog.i(getClass(), " #### IMAGE GET 시작 #### ");
			UtilLog.i(getClass(), " ####################### ");
			
			boolean isUpdatePicture = dbData.IsQuery(dbm, CommonConstants.FD_UPDATE);
			List<DataDB2>   updateList = new ArrayList<DataDB2>();
			List<Object> list = dbData.Select(dbm, CommonConstants.FD_IMAGE, new DataDB2());
			for(Object obj : list) {
				DataDB2 data = (DataDB2)obj;
				String userID   = data.GetData(CommonConstants.VAR_MEMID);
				if(UtilString.isEmpty(userID)) {
					userID   = data.GetData(CommonConstants.VAR_USER_ID);
				}
				String picture  = data.GetData(CommonConstants.VAR_PICTURE);
				String picUrl   = data.GetData(CommonConstants.VAR_PICURL);
				
				UtilLog.t(getClass(), String.format("%s["+userID+"] %s["+picture+"] %s["+picUrl+"]",
						CommonConstants.VAR_USER_ID, userID, CommonConstants.VAR_PICTURE, picture, CommonConstants.VAR_PICURL, picUrl));
				
				CfgImage img = Conf.getInstance().CheckCfgImage(userID);
				if(img == null) { continue; }
				if(downLoadImage(userID, picture, picUrl, img)) {
					if(isUpdatePicture) {
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_USER_ID, userID);
						newData.SetData(CommonConstants.VAR_PICTURE, picture);
						updateList.add(newData);
					}
				}
				thumbNail(picture);
				
				Thread.sleep(10);		
			}
			
			UtilLog.i(getClass(), " ####################### ");
			UtilLog.i(getClass(), " #### IMAGE GET 종료 #### ");
			UtilLog.i(getClass(), " ####################### ");
			
			if(updateList.size() > 0) {
				updatePhotoInfoInDb(updateList);
	    	}
			result.setResult(true);
			return result;
		} catch (Exception e) {
			UtilLog.e(getClass(), "{syncPicture} " + e);
		}
		result.setCause(BulkResult.BULK_UNKNOWN);
		return result;
	}

	@Override
	protected void onRecvMsg(Object obj) {
		if(obj instanceof RESTMessage) {
			RESTMessage msg = (RESTMessage)obj;
			BulkResult result = syncPicture(msg);
			if(result != null) {
				Bulk.send(Conf.getInstance().BULK_RESPONSE_URL(), Define.TID_DB_WORKER, result.isResult(), result.getCause(), msg);
			}
		}
	    //	BULK, BATCH DB 갱신 처리
	    if (obj instanceof DataBulk) {
	    	try {
	    		dbBulk((DataBulk)obj);
	    	} catch(Exception ignored) { }
	    }
	}    
	protected void onExit() {}
	protected void onInit() {}
	protected void onTimer(int arg0) {}
}