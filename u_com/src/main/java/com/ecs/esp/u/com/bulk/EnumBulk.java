package com.ecs.esp.u.com.bulk;

import com.ecs.msg.rest.custom.RESTMessage;

public enum EnumBulk {
	UNKNOWN(0),
	NATIVENAME_SYNC(1),					//	ONN
	PICTURE_SYNC(2),						//	IMG
	INSA_SYNC(3),							//	HR
	PHONEDO_SYNC(4),						//	HR
	USER(5),								//	HR
	TALK_ALERT(6);							//	ALERT

	int state	= 0;  
	
	EnumBulk(int state) {
    	this.state = state;
    }
    
	public int getState() {
    	return state;
    }
    
    public static EnumBulk valueOf(int value) {
    	for(EnumBulk d : EnumBulk.values()) {
    		if (d.getState() == value) return d;
    	}
    	return UNKNOWN;
    }
    
    public String getTitle() {
    	if(state == EnumBulk.NATIVENAME_SYNC.state) 	{   return "NativeName 동기화";					}
		if(state == EnumBulk.PICTURE_SYNC.state) 		{   return "사진 동기화";							}
		if(state == EnumBulk.INSA_SYNC.state) 			{   return "인사 동기화";							}
		if(state == EnumBulk.PHONEDO_SYNC.state) 		{   return "PHONEDO 동기화";						}
		if(state == EnumBulk.USER.state) 				{   return "USER 갱신";							}
		if(state == EnumBulk.TALK_ALERT.state) 			{   return "TALK ALERT 갱신";					}
		if(state == EnumBulk.UNKNOWN.state) 			{   return "UNKNOWN";							}
    	return "UNKNOWN";		
    }
    
    public String getURL() {
    	if(state == EnumBulk.NATIVENAME_SYNC.state) 	{   return RESTMessage.URL_SYNC_NATIVENAME;		}
    	if(state == EnumBulk.PICTURE_SYNC.state) 		{   return RESTMessage.URL_PICTURE_SYNC;		}
    	if(state == EnumBulk.INSA_SYNC.state) 			{   return RESTMessage.URL_SYNC_INSA;			}
    	if(state == EnumBulk.PHONEDO_SYNC.state) 		{   return RESTMessage.URL_SYNC_PHONEDO;		}
    	if(state == EnumBulk.USER.state) 				{   return RESTMessage.URL_USER;				}
		if(state == EnumBulk.TALK_ALERT.state) 			{   return RESTMessage.URL_TALK_ALERT;			}
		if(state == EnumBulk.UNKNOWN.state) 			{   return "UNKNOWN";							}
    	return "UNKNOWN";
    }
    
    public static EnumBulk getEnum(int messageID) {
        return switch (messageID) {
            case RESTMessage.REQ_REST_SYNC_NATIVENAME -> EnumBulk.NATIVENAME_SYNC;
            case RESTMessage.REQ_REST_PICTURE_SYNC -> EnumBulk.PICTURE_SYNC;
            case RESTMessage.REQ_REST_SYNC_INSA -> EnumBulk.INSA_SYNC;
            case RESTMessage.REQ_REST_SYNC_PHONEDO -> EnumBulk.PHONEDO_SYNC;
            case RESTMessage.REQ_REST_USER -> EnumBulk.USER;
            case RESTMessage.REQ_REST_TALK_ALERT -> EnumBulk.TALK_ALERT;
            default -> EnumBulk.UNKNOWN;
        };
    }
}