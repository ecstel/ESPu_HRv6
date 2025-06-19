function getReqValue() {
	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate = new Date();
	
	// 년, 월, 일
	var year = currentDate.getFullYear();
	var month = ('0' + (currentDate.getMonth() + 1)).slice(-2);
	var day = ('0' + currentDate.getDate()).slice(-2);
	
	// 시, 분, 초
	var hour = ('0' + currentDate.getHours()).slice(-2);
	var minute = ('0' + currentDate.getMinutes()).slice(-2);
	var second = ('0' + currentDate.getSeconds()).slice(-2);
	var millisecond = ('00' + currentDate.getMilliseconds()).slice(-3); 
	
	// 현재 시각 문자열 생성
	var currentTime = year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second + '.' + millisecond;
	$("#reqValue").append("요청시각 : " + currentTime+"\n\n");
}

function getRtnValue() {
	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate = new Date();
	
	// 년, 월, 일
	var year = currentDate.getFullYear();
	var month = ('0' + (currentDate.getMonth() + 1)).slice(-2);
	var day = ('0' + currentDate.getDate()).slice(-2);
	
	// 시, 분, 초
	var hour = ('0' + currentDate.getHours()).slice(-2);
	var minute = ('0' + currentDate.getMinutes()).slice(-2);
	var second = ('0' + currentDate.getSeconds()).slice(-2);
	var millisecond = ('00' + currentDate.getMilliseconds()).slice(-3); 
	
	// 현재 시각 문자열 생성
	var currentTime = year + '-' + month + '-' + day + ' ' + hour + ':' + minute + ':' + second  + '.' + millisecond;
	$("#rtnValue").append("응답시각 : " + currentTime+"\n\n");
}

function currentToken() {
	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate = new Date();
	
	// 년, 월, 일
	var year = currentDate.getFullYear();
	var month = ('0' + (currentDate.getMonth() + 1)).slice(-2);
	var day = ('0' + currentDate.getDate()).slice(-2);
	
	// 시, 분, 초
	var hour = ('0' + currentDate.getHours()).slice(-2);
	var minute = ('0' + currentDate.getMinutes()).slice(-2);
	var second = ('0' + currentDate.getSeconds()).slice(-2);
	var millisecond = ('00' + currentDate.getMilliseconds()).slice(-3); 
	
	// 현재 시각 문자열 생성
	var currentTime = year + month + day + hour + minute + second + millisecond;
	$("#TOKENVAL").append(currentTime);
}
function randomSEP() {

	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate1 = new Date();
	
	// 년, 월, 일
	var month = ('0' + (currentDate1.getMonth() + 1)).slice(-2);
	var day = ('0' + currentDate1.getDate()).slice(-2);
	
	// 시, 분, 초
	var hour = ('0' + currentDate1.getHours()).slice(-2);
	var minute = ('0' + currentDate1.getMinutes()).slice(-2);
	var second = ('0' + currentDate1.getSeconds()).slice(-2);
	
	// 현재 시각 문자열 생성
	var currentMAC = month + day + hour + minute + second;
	$("#RANDOMSEP1").append(currentMAC);
}
function randomDN() {

	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate1 = new Date();
	
	// 분, 초
	var minute  = ('0' + currentDate1.getMinutes()).slice(-2);
	var second1 = ('0' + currentDate1.getSeconds()+1).slice(-2);
	var second2 = ('0' + currentDate1.getSeconds()+2).slice(-2);

	// 현재 시각 문자열 생성
	var currentDN1  = minute + second1;
	var currentDN2  = minute + second2;

	$("#RANDOMDN11").append(currentDN1);
	$("#RANDOMDN12").append(currentDN2);
	$("#RANDOMDN21").append(currentDN1);
	$("#RANDOMDN22").append(currentDN2);
}
function randomDIR() {

// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate1 = new Date();
	
	// 분, 초
	var minute  = ('0' + currentDate1.getMinutes()).slice(-2);
	var second1 = ('0' + currentDate1.getSeconds()+1).slice(-2);

	// 현재 시각 문자열 생성
	var currentDN1  = minute + second1;
	$("#RANDOMDIR1").append(currentDN1);
}
function randomPG() {

	// 현재 날짜와 시간을 포함하는 Date 객체 생성
	var currentDate1 = new Date();
	
	// 분, 초
	var minute  = ('0' + currentDate1.getMinutes()).slice(-2);
	var second1 = ('0' + currentDate1.getSeconds()+1).slice(-2);
	var second2 = ('0' + currentDate1.getSeconds()+2).slice(-2);


	// 현재 시각 문자열 생성
	var currentPG1  = minute + second1;
	var currentPG2  = minute + second2;

	$("#RADOM_NAME1").append(currentPG1);
	$("#RADOM_PATTERN1").append(currentPG1);

	$("#RADOM_NAME21").append(currentPG1);
	$("#RADOM_PATTERN21").append(currentPG1);

	$("#RADOM_NAME22").append(currentPG2);
	$("#RADOM_PATTERN22").append(currentPG2);

	$("#RADOM_NAME31").append(currentPG1);
	$("#RADOM_PATTERN31").append(currentPG1);
	$("#RADOM_NAME32").append(currentPG2);
	$("#RADOM_PATTERN32").append(currentPG2);

	$("#RADOM_NAME4").append(currentPG1);
	$("#RADOM_PATTERN4").append(currentPG1);
}