
###############################################################
## [ 디렉토리 구조 ] #############################################
###############################################################
	 	
━ file : 파일을 통한 동기화시 사용할 파일 목록
   ┣━ batch : 일배치를 위한 파일 목록
   ┃    ┗━━━━ ex : 도로공사
   ┃          	
   ┗━ rtime : 실시간 인사 변경 파일 목록
     	┗━━━━ kbsc : KB증권
     	
━ query : 인사질의 파일
   ┣━ db2db : 원격지 DB에 접속하여 로컬 DB에 동기화하는 경우
   ┃    ┣━ MULTIPLE : USER,DEPT,GRADE,POSITION 등 다수의 질의문을 이용한 동기화
   ┃    ┃      ┣━ query_local_postgres.xml
   ┃    ┃      ┗━ query_remote_postgres.xml
   ┃    ┃
   ┃    ┗━ SINGLE : 하나의 질의문을 통한 동기화
   ┃           ┣━ query_local_postgres.xml
   ┃           ┗━ query_remote_postgres.xml
   ┃
   ┣━ file2db : 파일을 통한 로컬 DB에 동기화하는 경우
   ┃    ┗━ MULTIPLE : USER,DEPT 등 다수의 파일을 이용한 동기화
   ┃           ┣━ query_local_postgres.xml
   ┃           ┗━ query_remote_postgres.xml
   ┃
   ┣━ hdb : 사용자 내선번호 갱신, 내선중복체크, PhoneDO체크, 사이트역방향동기화 처리
   ┃    ┗━ hdb_local_postgres.xml
   ┃
   ┗━ site : 사이트에서 사용하는 질의문
        ┣━ ex : 도로공사
        ┗━ sinhanez : 신한Ez



        