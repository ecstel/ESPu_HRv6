package com.ecs.esp.u.com.tester;

import static io.netty.handler.codec.http.HttpHeaderNames.CACHE_CONTROL;
import static io.netty.handler.codec.http.HttpHeaderNames.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaderNames.DATE;
import static io.netty.handler.codec.http.HttpHeaderNames.EXPIRES;
import static io.netty.handler.codec.http.HttpHeaderNames.IF_MODIFIED_SINCE;
import static io.netty.handler.codec.http.HttpHeaderNames.LAST_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_MODIFIED;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.File;
import java.io.RandomAccessFile;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

import javax.activation.MimetypesFileTypeMap;

import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelProgressiveFuture;
import io.netty.channel.ChannelProgressiveFutureListener;
import io.netty.channel.DefaultFileRegion;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResultProvider;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpResponse;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpChunkedInput;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.HttpUtil;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.stream.ChunkedFile;
import io.netty.util.CharsetUtil;

public class TesterHandler  extends SimpleChannelInboundHandler<FullHttpRequest> {
	/********************************************************************
	 * Data
	 ********************************************************************/
	private static final int HTTP_CACHE_SECONDS = 60;
    private static final String HTTP_DATE_GMT_TIMEZONE = "GMT";
    private static final String HTTP_DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final Pattern INSECURE_URI = Pattern.compile(".*[<>&\"].*");

    private static String menuFile = "";
    private static String defaultPath = "./";
    private static boolean useAllowIP = false;
    private static boolean useAllowPath = false;
    private static final List<String> ipList = new ArrayList<String>();
    private static final List<String> keyEntry = new ArrayList<String>();
    private static final Map<String, String> pathMap = new HashMap<String, String>();

	/********************************************************************
	 * GetIPAddress
	 ********************************************************************/
    public static String GetIPAddress(ChannelHandlerContext ctx) {
    	try {
            InetSocketAddress inet = (InetSocketAddress)ctx.channel().remoteAddress();
            return inet.getAddress().getHostAddress();
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return null;
    }
    
	/********************************************************************
	 * Static KeyEntry
	 ********************************************************************/
    public static boolean PutKeyEntry(String entry) {
    	try {
    		if(!keyEntry.contains(entry)) {
    			keyEntry.add(entry);
    			return true;
    		}
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    public static boolean ClearKeyEntry(String ipaddr) {
    	try {
    		keyEntry.clear();
    		pathMap.clear();
			return true;
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    
	/********************************************************************
	 * Static IPAddress
	 ********************************************************************/
    public static void SetAllowIP(boolean allow) { useAllowIP = allow; }
    public static boolean PutIPAddress(String ipaddr) {
    	try {
    		if(!ipList.contains(ipaddr)) {
    			ipList.add(ipaddr);
    			return true;
    		}
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    public static boolean PopIPAddress(String ipaddr) {
    	try {
    		ipList.remove(ipaddr);
			return true;
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    public static String GetAllowIP(String ipaddr) {
    	try {
			if(useAllowIP) {
				if(ipList.contains(ipaddr)) {
					return ipaddr;
				}
			} else {
				return ipaddr;
			}
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return null;
    }
    
	/********************************************************************
	 * Static KeyPath
	 ********************************************************************/
    public static void SetMenuFile(String file) { menuFile = file; }
    public static void SetDefaultPath(String path) { defaultPath = path; }
    public static void SetAllowPath(boolean allow) { useAllowPath = allow; }
    public static boolean PutKeyPath(String key, String path) {
    	try {
			if(pathMap.containsKey(key)) { return false; }
			pathMap.put(key, path);
			return true;
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    public static boolean PopKeyPath(String key) {
    	try {
    		pathMap.remove(key);
			return true;
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e); }
    	return false;
    }
    public static String GetKeyPath(String key) {
    	try {
			if(useAllowPath) {
				if(pathMap.containsKey(key)) {
					return pathMap.get(key);
				}
			} else {
				return defaultPath;
			}
		} catch (Exception ignored) { }
    	return null;
    }
    
	/********************************************************************
	 * SendError
	 ********************************************************************/
    private void SendError(ChannelHandlerContext ctx, HttpResponseStatus status) {
    	try {
        	UtilLog.e(getClass(), "FILE_REQUEST:" + status, GetIPAddress(ctx));
    		ByteBuf buf = Unpooled.copiedBuffer("Failure: " + status + "\r\n", CharsetUtil.UTF_8);
            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, buf);
            response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");
            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e);}
    }
    private void SendDownloadPage(ChannelHandlerContext ctx) {
    	try {
    		File file = new File(defaultPath + File.separator + menuFile);
    		if(!file.exists()) {
    			SendError(ctx, NOT_FOUND);
    		} else {
	        	UtilLog.i(getClass(), "DOWNLOAD PAGE:" + HttpResponseStatus.OK, GetIPAddress(ctx));
	    		ByteBuf buf = Unpooled.copiedBuffer(new String(UtilFile.getBytesFromFile(file), CharsetUtil.UTF_8), CharsetUtil.UTF_8);
	            FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, HttpResponseStatus.OK, buf);
	            ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    		}
		} catch (Exception e) { UtilLog.e(TesterHandler.class, e);}
    }

	/********************************************************************
	 * CheckHttpRequest
	 ********************************************************************/
    private boolean CheckHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
    	try {
    		String remoteIP = GetIPAddress(ctx);
    		if(!((DecoderResultProvider)request).decoderResult().isSuccess()) {
                SendError(ctx, BAD_REQUEST);
                return false;
            }

            if(remoteIP == null) {
            	SendError(ctx, FORBIDDEN);
                return false;
            }
            if(!remoteIP.equals(GetAllowIP(remoteIP))) {
            	SendError(ctx, FORBIDDEN);
                return false;
            }
            return true;
		} catch (Exception ignored) { }
    	return false;
    }
    
	/*****************************************************************
	 * GetPath
	 *****************************************************************/
	public String GetPath(HttpRequest req) {
		try {
			QueryStringDecoder qsDecoder = new QueryStringDecoder(req.uri());
			return qsDecoder.path();
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return "";
	}
    
	/*****************************************************************
	 * GetParameter
	 *****************************************************************/
	public String GetParameter(HttpRequest req) {
		try {
			if(keyEntry.size() <= 0) { return null; }
			
			StringBuilder key = new StringBuilder();
			QueryStringDecoder qsDecoder = new QueryStringDecoder(req.uri());
            Map<String, List<String>> params = qsDecoder.parameters();
            
            for(String entry : keyEntry) {
	            if(params.containsKey(entry)) {
	            	List<String> vals = params.get(entry);
	                for(String val : vals) {
	                	if(UtilString.isEmpty(val)) { continue; }
	                	if(!key.isEmpty()) { key.append("_"); }
	                	key.append(val);
	                	break;
	                }
	            }
            }
            return key.toString();
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return "";
	}

	/********************************************************************
	 * GetFile
	 ********************************************************************/
    private File GetFile(ChannelHandlerContext ctx, FullHttpRequest request) {
        try {
            String uri = GetPath(request);
            String key = GetParameter(request);
            String path = GetKeyPath(key);

        	UtilLog.d(getClass(), "FILE_URI", uri);
        	
            if(path == null) { SendError(ctx, FORBIDDEN); return null; }
            
            uri = URLDecoder.decode(uri, StandardCharsets.UTF_8).replace('/', File.separatorChar);
            if(uri.charAt(0) == '.') { 
            	SendError(ctx, NOT_FOUND); 
            	return null; 
            }
            if(uri.contains(File.separator + '.')) { 
            	SendError(ctx, NOT_FOUND);
            	return null; 
            }
            if(uri.contains('.' + File.separator)) { 
            	SendError(ctx, NOT_FOUND); 
            	return null; 
            }
            if(uri.charAt(uri.length() - 1) == '.') { 
            	SendError(ctx, NOT_FOUND);
            	return null; 
            }
            if(INSECURE_URI.matcher(uri).matches()) {
            	SendError(ctx, NOT_FOUND); 
            	return null; 
            }
            File file = new File(path + File.separator + uri);
            UtilLog.i(getClass(), "FILE_PATH", file.getAbsolutePath());
            if(!file.isFile()) { 
            	SendError(ctx, NOT_FOUND);
            	return null;
            }
            if(!file.exists()) { 
            	SendError(ctx, NOT_FOUND); 
            	return null;
            }
            if(file.isHidden()) { 
            	SendError(ctx, FORBIDDEN); 
            	return null;
            }
            return file;
		} catch (Exception e) { UtilLog.e(getClass(), e); }
        return null;
    }

	/********************************************************************
	 * SendNotModified
	 ********************************************************************/
    private void SendNotModified(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, NOT_MODIFIED);
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
        dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

        Calendar time = new GregorianCalendar();
        response.headers().set(DATE, dateFormatter.format(time.getTime()));
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

	/********************************************************************
	 * CheckFileDate
	 ********************************************************************/
    private boolean CheckFileDate(ChannelHandlerContext ctx, FullHttpRequest request, File file) {
    	try {
    		String ifModifiedSince = request.headers().get(IF_MODIFIED_SINCE);
	        if(ifModifiedSince != null && !ifModifiedSince.isEmpty()) {
	            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
	            Date ifModifiedSinceDate = dateFormatter.parse(ifModifiedSince);
	            long ifModifiedSinceDateSeconds = ifModifiedSinceDate.getTime() / 1000;
	            long fileLastModifiedSeconds = file.lastModified() / 1000;
	            if (ifModifiedSinceDateSeconds == fileLastModifiedSeconds) {
	            	SendNotModified(ctx);
	                return false;
	            }
	        }
	        return true;
		} catch (Exception ignored) { }
    	return false;
    }

	/********************************************************************
	 * SendFileHeader
	 ********************************************************************/
    private boolean SendFileHeader(ChannelHandlerContext ctx, HttpRequest request, File file, RandomAccessFile randFile) {
    	try {
	        HttpResponse response = new DefaultHttpResponse(HTTP_1_1, OK);
	        HttpUtil.setContentLength(response, randFile.length());	
	        
    		MimetypesFileTypeMap mimeTypesMap = new MimetypesFileTypeMap();
            response.headers().set(CONTENT_TYPE, mimeTypesMap.getContentType(file.getPath()));
            
            SimpleDateFormat dateFormatter = new SimpleDateFormat(HTTP_DATE_FORMAT, Locale.US);
            dateFormatter.setTimeZone(TimeZone.getTimeZone(HTTP_DATE_GMT_TIMEZONE));

            Calendar time = new GregorianCalendar();
            response.headers().set(DATE, dateFormatter.format(time.getTime()));

            time.add(Calendar.SECOND, HTTP_CACHE_SECONDS);
            response.headers().set(EXPIRES, dateFormatter.format(time.getTime()));
            response.headers().set(CACHE_CONTROL, "private, max-age=" + HTTP_CACHE_SECONDS);
            response.headers().set(LAST_MODIFIED, dateFormatter.format(new Date(file.lastModified())));

            if(HttpUtil.isKeepAlive(request)) {
	            response.headers().set(CONNECTION, HttpHeaderValues.KEEP_ALIVE); // HttpHeaders.Values.KEEP_ALIVE);
	        }
	        
        	ctx.write(response);
        	return true;
		} catch (Exception ignored) { }
        return false;
    }

	/********************************************************************
	 * SendFileHeader
	 ********************************************************************/
    private ChannelFuture SendFileStream(ChannelHandlerContext ctx, RandomAccessFile randFile) {
    	try {
	        ChannelFuture cfSend;
	        ChannelFuture cfLast;
	        long length = randFile.length();
	        if (ctx.pipeline().get(SslHandler.class) == null) {
	            cfSend = ctx.write(new DefaultFileRegion(randFile.getChannel(), 0, length), ctx.newProgressivePromise());
	            cfLast = ctx.writeAndFlush(LastHttpContent.EMPTY_LAST_CONTENT);
	        } else {
	            cfSend = ctx.writeAndFlush(new HttpChunkedInput(new ChunkedFile(randFile, 0, length, 8192)), ctx.newProgressivePromise());
	            cfLast = cfSend;
	        }

	        cfSend.addListener(new ChannelProgressiveFutureListener() {
	            @Override
	            public void operationProgressed(ChannelProgressiveFuture future, long progress, long total) { }

	            @Override
	            public void operationComplete(ChannelProgressiveFuture future) {
	            	try{
	            		future.channel().close().sync();
	            	} catch(Exception ex) { UtilLog.e(TesterHandler.class, ex); }
                	UtilLog.i(TesterHandler.class, "FILE_REQUEST:COMPLETED", future.channel().remoteAddress().toString());
	            }
	        });
	        return cfLast;
		} catch (Exception ignored) { }
        return null;
    }

	/********************************************************************
	 * SendFile
	 ********************************************************************/
	private void SendFile(ChannelHandlerContext ctx, FullHttpRequest request, File file) {
		try {
	        if(!CheckFileDate(ctx, request, file)) { return; }

	        RandomAccessFile randFile = new RandomAccessFile(file, "r");
	        if(SendFileHeader(ctx, request, file, randFile)) { 
		        ChannelFuture lastContentFuture = SendFileStream(ctx, randFile);
		        if (!HttpUtil.isKeepAlive(request)) {
		            Objects.requireNonNull(lastContentFuture).addListener(ChannelFutureListener.CLOSE);
		        }
	        }
		} catch (Exception e) { UtilLog.e(getClass(), e); }
	}
	
	/********************************************************************
	 * channelRead0
	 ********************************************************************/
    @Override
    public void channelRead0(ChannelHandlerContext ctx, FullHttpRequest request) throws Exception {
    	try {
    		String uri = GetPath(request);
    		UtilLog.t(getClass(), "APP_REQUEST " + GetIPAddress(ctx) + ", URI=" + uri);
    		if(uri.equals("/")) {
    			SendDownloadPage(ctx);
    		} else {
    			if(!CheckHttpRequest(ctx, request)) { return; }
    	        File file = GetFile(ctx, request);
    	        if(file != null) { SendFile(ctx, request, file); }
    		}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
    }

	/********************************************************************
	 * exceptionCaught
	 ********************************************************************/
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
	//	UtilLog.e(getClass(), cause);
	    if(ctx.channel().isActive()) {
            SendError(ctx, INTERNAL_SERVER_ERROR);
        } else {
        	ctx.close();
        }
    }
}