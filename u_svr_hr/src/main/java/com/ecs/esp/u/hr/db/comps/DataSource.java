package com.ecs.esp.u.hr.db.comps;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.EDBPostgres;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.db2.data.EDBColumn;
import com.ecs.base.db2.data.EDBExpandData;
import com.ecs.base.db2.data.EDBResult;
import com.ecs.base.db2.data.EDBSql;
import com.ecs.base.db2.exception.EDBException;
import com.ecs.esp.u.hr.define.Conf;

public class DataSource extends EDBExpandData {
	/********************************************************************
	 * Constructor
	 ********************************************************************/
	public DataSource(String path) throws FileNotFoundException, EDBException {
		super(path);
		UtilLog.i(getClass(), String.format("\n\t\t\t\t PATH[%s] \n\t\t\t\t DATABASE[%s] HOSTNAME[%s] DBNAME[%s] DBUSER[%s] DBFILEPATH[%s] \n",
				path, DATABASE(), HOSTNAME(), DBNAME(), DBUSER(), DBFILEPATH()));
	}
	
	/********************************************************************
	 * Initialize
	 ********************************************************************/
	public boolean Init(EDBM dbm) {
		try {
			if(dbm == null) { return false; }
			return InitCreate(dbm);
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}

	/********************************************************************
	 * CompareData
	 * 	List<Object> insertList = new ArrayList<Object>();
	 *	List<Object> updateList = new ArrayList<Object>();
	 *	List<Object> deleteList = new ArrayList<Object>();
	 *	List<String> compareList = new ArrayList<String>();
	 *	compareList.add("USER_CODE");
	 *	compareList.add("USER_NAME");
	 *	List<Object> resultList = dbData.Select(dbm, "GET_USERINFO", realList);
	 *	if (dbData.CompareData(realList, resultList, compareList, insertList, updateList, deleteList)) {
	 *		dbData.Insert(dbm, "INSERT_USERINFO", insertList);
	 *		dbData.Update(dbm, "UPDATE_USERINFO", updateList);
	 *		dbData.Delete(dbm, "DELETE_USERINFO", deleteList);
	 *	}
	 ********************************************************************/
	protected boolean CompareData(
            List<?> targetList,        			//	비교대상
            List<?> dbResultList,    			//	DB질의결과(현재의값)
            List<String> compareList,           //	DB에서 비교할 항목
            List<Object> insertList , List<Object> updateList, List<Object> deleteList)	//	결과를 담을 객체
	{
		try {

			for(Object data1 : targetList) {				//	TARGET
				EDBResult tData = (EDBResult)data1;
				boolean isExist = false;
				for(Object  data : dbResultList) {			//	DATABASE
					if(data instanceof EDBResult dData) {
                        int equalsLen  = 0;
		    			int compareLen = compareList.size();
                        for (String s : compareList) {
                            String t = dData.GetData(s);
                            String d = tData.GetData(s);
                            if (!UtilString.isEmpty(t) && !UtilString.isEmpty(d) && !t.equals(d)) {
                                break;
                            }
                            equalsLen++;
                        }
		    			if(equalsLen == compareLen) {
		    				UtilLog.t(getClass(), "UPDATE="+tData.GetMap().toString());
		    				updateList.add( tData );
		    				isExist = true;
		    				break;
		    			}
		    		}
				}
				if(!isExist) {
					insertList.add( tData );
					UtilLog.t(getClass(), "INSERT="+tData.GetMap().toString());
				} 
			}
			for(Object  data : dbResultList) {				//	DATABASE
	    		boolean isExist = false;
	    		if(data instanceof EDBResult dData) {
                    for(Object data1 : targetList) {		//	TARGET
	    				int equalsLen  = 0;
		    			int compareLen = compareList.size();
		    			EDBResult tData = (EDBResult)data1;
                        for (String s : compareList) {
                            //	UtilLog.t(getClass(), "t = > " + dData.toMapString());
                            String t = dData.GetData(s);
                            String d = tData.GetData(s);
                            //	UtilLog.i(getClass(), "t="+t+", "+compareList.get(i));
                            //	UtilLog.i(getClass(), "d="+d+", "+compareList.get(i));
                            //	[09-01 09:50:19] INFO - [          DataSource]### t=null, SITE
                            //	[09-01 09:50:19] INFO - [          DataSource]### d=NTS, SITE
                            //	[09-01 09:50:19]ERROR - [          DataSource]###
                            if (!t.equals(d)) {
                                break;
                            }
                            equalsLen++;
                        }
		    			if(equalsLen == compareLen) {
		    				isExist = true;
							break;
		    			}
	    			}
	    			if(!isExist) {
						UtilLog.t(getClass(), "DELETE="+dData.GetMap().toString());
	    				deleteList.add(dData);	
	    			}
	    		}
	    	}
	    	return true;
	    } catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	
	/********************************************************************
	 * InitCreate : Sequence, Table, Index, Comment(실행시 계속 갱신됨)
	 ********************************************************************/
	public boolean InitCreate(EDBM dbm) { 
		try {
			String startMonth = UtilCalendar.getMonthDay(Conf.getInstance().START_TABLE(), (EDBColumn.YYYYMM)); // 년월
			String endMonth = UtilCalendar.getMonthDay(Conf.getInstance().END_TABLE(), (EDBColumn.YYYYMM));		// 년월
			List<String> dateList = UtilCalendar.getMonthList(startMonth, endMonth, EDBColumn.YYYYMM);
			String startDay = UtilCalendar.getDay(Conf.getInstance().START_TABLE_DAY(), EDBColumn.YYYYMMDD);	//	년월일
			String endDay = UtilCalendar.getDay(Conf.getInstance().END_TABLE_DAY(), EDBColumn.YYYYMMDD);		//	년월일
			
			//	테이블, 뷰, index, seq 생성
			if (Conf.getInstance().START_TABLE_DAY() < (365*3)+1 && Conf.getInstance().END_TABLE_DAY() < (365*3)+1) {		
				List<String> dayList = UtilCalendar.getDateList(startDay, endDay);
				CreateTable(dbm, dateList, dayList);
			} else {
				CreateTable(dbm, dateList, null);
			}
			//	함수 생성
			for(String func : GetFunction()) {
				if(CreateFunction(dbm, func, null)) {
					UtilLog.t(getClass(), "CreateFunction success.");
				}
			}
			for(String view : GetViewFirst()) {
				if (CreateView(dbm, view, null)) {
					UtilLog.t(getClass(), "CreateView success.");
				}
			}
			//	기본값 등록
			if (Conf.getInstance().START_TABLE_DAY() < (365*3)+1 && Conf.getInstance().END_TABLE_DAY() < (365*3)+1) {		
				List<String> dayList = UtilCalendar.getDateList(startDay, endDay);
				for(String name : GetBasic()) {
					if(InsertBasic(dbm, name, dateList, dayList)) {
						UtilLog.t(getClass(), "InsertBasic success.");
					}
				}
			} else {
				for(String name : GetBasic()) {
					if(InsertBasic(dbm, name, dateList, null)) {
						UtilLog.t(getClass(), "InsertBasic success.");
					}
				}
			}
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return true;
	}
	
	/********************************************************************
	 * CreateTable
	 ********************************************************************/
	private void CreateTable(EDBM dbm, List<String> dateList, List<String> dayList) {
		try {
			if(dateList != null ) {
				UtilLog.i(getClass(), String.format("CREATE MONTH LIST => [%s]", UtilString.TrimList(dateList)));
			}
			if(dayList != null ) {
				UtilLog.i(getClass(), String.format("CREATE DAY LIST => [%s]", UtilString.TrimList(dayList)));
			}

			ArrayList<String> createList = GetCreate();		//	name => CREATE_XXXX
			for(String name : createList) {
				
				if(name.contains(EDBColumn.YYYYMMDD)) {
					if(dayList != null) {
						for(String day : dayList) {
							Create(dbm, name, EDBColumn.YYYYMMDD, day);	
						}
					}
				} else if(name.contains(EDBColumn.YYYYMM)) {
					if(dateList != null) {
						for(String date : dateList) {
							Create(dbm, name, EDBColumn.YYYYMM, date);	
						}
					}
				} else {
					Create(dbm, name);
				}
			}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
	}
	public boolean CreateTable(EDBM dbm, String owner, String tableName, String sql) {
		try {
            if (UtilString.isEmpty(owner)) {
                return CreateTableSql(dbm, tableName, sql);
            }
			return CreateTableSql(dbm, owner, tableName, sql);
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}

	/**********************************************************************************************************
	 * CreateTable : 기본 테이블 자동 생성
	 **********************************************************************************************************/
	public boolean createTable(EDBM dbm, String schema, String tableName, List<String> cellList) {
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
				if(UtilString.isEmpty(schema)) {
					createHead = "CREATE TABLE %s ( ";
				} else {
					createHead = "CREATE TABLE %s.%s ( ";
				}
				createBody = "%s VARCHAR(512), ";
				createTime = "UPDATETIME VARCHAR(64) ";
				createEnd  = ")";		
			}
			StringBuilder body = new StringBuilder();
			for ( String data : cellList) {
				body.append(String.format(createBody, data));
				if(UtilString.isEmpty(data)) {
					return false;
				}
			}
			String sql =  "";
			if(UtilString.isEmpty(schema)) {
				sql = String.format(createHead, tableName);
			} else {
				sql = String.format(createHead, schema, tableName);
			}
			sql += body;	//	UtilCom.replaceLast(body, ",", "");
			sql += createTime;
			sql += createEnd;
			CreateTable(dbm, schema, tableName, sql);
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	/**********************************************************************************************************
	 * detectStringCharset
	 **********************************************************************************************************/
	protected void detectStringCharset(String word) {
		try {
	        // SOURCE_CHARSET 값이 "ShowWord"이면 별도 처리를 함 (디버그용 혹은 로깅용)
	        if ("CHECK".equalsIgnoreCase( Conf.getInstance().SOURCE_CHARSET() )) {
	        	UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "utf-8 -> utf-8			: " + new String(word.getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8));
				UtilLog.i(getClass(), "utf-8 -> euc-kr			: " + new String(word.getBytes(StandardCharsets.UTF_8), "euc-kr"));
				UtilLog.i(getClass(), "utf-8 -> ksc5601			: " + new String(word.getBytes(StandardCharsets.UTF_8), "ksc5601"));
				UtilLog.i(getClass(), "utf-8 -> x-windows-949		: " + new String(word.getBytes(StandardCharsets.UTF_8), "x-windows-949"));
				UtilLog.i(getClass(), "utf-8 -> iso-8859-1			: " + new String(word.getBytes(StandardCharsets.UTF_8), StandardCharsets.ISO_8859_1));
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "iso-8859-1 -> iso-8859-1		: " + new String(word.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.ISO_8859_1));
				UtilLog.i(getClass(), "iso-8859-1 -> euc-kr			: " + new String(word.getBytes(StandardCharsets.ISO_8859_1), "euc-kr"));
				UtilLog.i(getClass(), "iso-8859-1 -> ksc5601			: " + new String(word.getBytes(StandardCharsets.ISO_8859_1), "ksc5601"));
				UtilLog.i(getClass(), "iso-8859-1 -> x-windows-949		: " + new String(word.getBytes(StandardCharsets.ISO_8859_1), "x-windows-949"));
				UtilLog.i(getClass(), "iso-8859-1 -> utf-8			: " + new String(word.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8));
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "euc-kr -> euc-kr			: " + new String(word.getBytes("euc-kr"), "euc-kr"));
				UtilLog.i(getClass(), "euc-kr -> utf-8			: " + new String(word.getBytes("euc-kr"), StandardCharsets.UTF_8));
				UtilLog.i(getClass(), "euc-kr -> ksc5601			: " + new String(word.getBytes("euc-kr"), "ksc5601"));
				UtilLog.i(getClass(), "euc-kr -> x-windows-949		: " + new String(word.getBytes("euc-kr"), "x-windows-949"));
				UtilLog.i(getClass(), "euc-kr -> iso-8859-1			: " + new String(word.getBytes("euc-kr"), StandardCharsets.ISO_8859_1));
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "ksc5601 -> ksc5601			: " + new String(word.getBytes("ksc5601"), "ksc5601"));
				UtilLog.i(getClass(), "ksc5601 -> euc-kr			: " + new String(word.getBytes("ksc5601"), "euc-kr"));
				UtilLog.i(getClass(), "ksc5601 -> utf-8			: " + new String(word.getBytes("ksc5601"), StandardCharsets.UTF_8));
				UtilLog.i(getClass(), "ksc5601 -> x-windows-949		: " + new String(word.getBytes("ksc5601"), "x-windows-949"));
				UtilLog.i(getClass(), "ksc5601 -> iso-8859-1			: " + new String(word.getBytes("ksc5601"), StandardCharsets.ISO_8859_1));
				UtilLog.i(getClass(), "");
				UtilLog.i(getClass(), "x-windows-949 -> x-windows-949	: " + new String(word.getBytes("x-windows-949"), "x-windows-949"));
				UtilLog.i(getClass(), "x-windows-949 -> euc-kr     		: " + new String(word.getBytes("x-windows-949"), "euc-kr"));
				UtilLog.i(getClass(), "x-windows-949 -> utf-8      		: " + new String(word.getBytes("x-windows-949"), StandardCharsets.UTF_8));
				UtilLog.i(getClass(), "x-windows-949 -> ksc5601    		: " + new String(word.getBytes("x-windows-949"), "ksc5601"));
				UtilLog.i(getClass(), "x-windows-949 -> iso-8859-1 		: " + new String(word.getBytes("x-windows-949"), StandardCharsets.ISO_8859_1));
				System.exit(0);
			}
		} catch(Exception e) { UtilLog.e(getClass(), e); }
	}	
	/**********************************************************************************************************
	 * 데이터 입력
	 **********************************************************************************************************/
	protected boolean insertTable(EDBM dbm, String tableName, List<String> cellList, List<String> dataList, String yyyyMMdd) throws Exception {
	    try {
	        // SQL 구문 템플릿
	        String sqlTemplate = "INSERT INTO %s ( %s, %s ) VALUES ( %s, '%s' )";
	        
	        // 데이터 리스트에서 값을 하나의 문자열로 생성 (각 값은 작은따옴표로 감쌈)
	        StringBuilder valuesBuilder = new StringBuilder();
	        for (String data : dataList) {
	            // 실제 운영환경에서는 SQL 인젝션 방지를 위해 값 이스케이프 처리가 필요함
	            valuesBuilder.append(String.format("'%s', ", data));
	        }
	        
	        // 마지막의 ", " 제거
	        if (!valuesBuilder.isEmpty()) {
	            valuesBuilder.setLength(valuesBuilder.length() - 2);
	        }
	        String values = valuesBuilder.toString();
	        detectStringCharset(values);
	        
	        // 원본과 대상 charset 모두 값이 있으면 인코딩 변환 처리
	        String sourceCharset = Conf.getInstance().SOURCE_CHARSET();
	        String targetCharset = Conf.getInstance().TARGET_CHARSET();
	        if (!UtilString.isEmpty(sourceCharset) && !UtilString.isEmpty(targetCharset)) {
	            try {
	            	values = new String(values.getBytes(sourceCharset), targetCharset);
	            } catch (UnsupportedEncodingException e) {
	                UtilLog.e(getClass(), e);
	                return false;
	            }
	        }
	        // cellList를 트림 처리하여 컬럼 목록을 생성
	        String columns = UtilString.TrimList(cellList);
	        String query = String.format(sqlTemplate, tableName,columns,  "UPDATETIME", values, yyyyMMdd);		//	테이블에 UPDATETIME 강제로 생성함
	        
	        // 생성된 SQL 구문 실행
	        return Execute(dbm, query);
	    } catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::insertTable()} " + e.getMessage());}
	}	
	
	/********************************************************************
	 * InsertBasic
	 ********************************************************************/
	public boolean InsertBasic(EDBM dbm, String name, List<String> dateList, List<String> dayList) {
		try {
			if(dateList != null) {
				for(String date : dateList) {
					if(InsertBasic(dbm, name, EDBColumn.YYYYMM, date)) {
						UtilLog.t(getClass(), "InsertBasic success.");
					}
				}
			}
			if(dayList != null) {
				for(String date : dayList) {
					if(InsertBasic(dbm, name, EDBColumn.YYYYMMDD, date)) {
						UtilLog.t(getClass(), "InsertBasic success.");
					}
				}
			}
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e ); }
		return false;
	}
	/********************************************************************
	 * InsertBasic
	 ********************************************************************/
	private boolean InsertBasic(EDBM dbm, String name, String fmt, String date) {
		String sql = "";
		try {
			int inx = 0;
			List<Integer> inxList = new ArrayList<Integer>();
			DataDB2 put = new DataDB2();
			put.SetData(fmt, date);
			List<EDBSql> isList = GetQueryExpandBasic(name, put);
			for(EDBSql edbSQL : isList ) {
				sql = edbSQL.getSql();
				if(!dbm.IsExist(edbSQL)) {
					inxList.add(inx);
				}
				inx++;
			}
			List<EDBSql> list = GetQueryExpandBasic(name, put, inxList);
			if(list != null) {
				for(EDBSql edbSQL : list ) {
					try {
						sql = edbSQL.getSql();
						dbm.Execute(edbSQL);
					} catch(Exception e) { UtilLog.e(getClass(), e); }
				}
			}
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e + " SQL=" + sql); }
		return false;
	}
	public boolean CreateFunction(EDBM dbm, String name, String date) {
		try {
			String sql = GetQueryExpand(name);
			if (!UtilString.isEmpty(sql)) {
				if(!IsExistFunction(dbm, name, null, null)) {
					UtilLog.t(getClass(), sql);
					dbm.Execute(new EDBSql(sql));
				}
			}
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	public boolean CreateView(EDBM dbm, String name, String date) {
		try {
			String sql = GetQueryExpand(name);
			if (!IsExistView(dbm, name)) {
				UtilLog.t(getClass(), sql);
				dbm.Execute(new EDBSql(sql));
			}
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
		
	/********************************************************************
	 * DataList
	 ********************************************************************/
	public List<EDBResult> DataList(Object obj) {
		try {
			List<EDBResult> list = null;
			if (obj instanceof List) {
				list = new ArrayList<EDBResult>();
				for (int i = 0; i < ((List<?>) obj).size(); i++) {
					Object item = ((List<?>) obj).get(i);
					if (item instanceof DataDB2) {
						list.add((DataDB2) item);
					} else {
						UtilLog.e(getClass(), "{DataList} NoClassDefFoundError. {" + item.getClass().getName() +"}");
					}
				}
			} else {
				list = new ArrayList<EDBResult>();
				if (obj instanceof DataDB2) {
					list.add((DataDB2) obj);
				} else {
					UtilLog.e(getClass(), "{DataList} NoClassDefFoundError. {" + obj.getClass().getName() +"}");
				}
			}
			return list;
		} catch(Exception ignored) { }
		return null;
	}

	/********************************************************************
	 * IsQuery
	 ********************************************************************/
	public boolean IsQuery(EDBM dbm, String field) {
		try {
			String data = GetQuery(field);
			if(!UtilString.isEmpty(data)) {
				return true;
			}
		} catch (Exception ignored) {}
		return false;
	}
	
	public String GetQuery(EDBM dbm, String field) {
		try {
			String data = GetQuery(field);
			if(!UtilString.isEmpty(data)) {
				return data;
			}
		} catch (Exception ignored) {}
		return "";
	}

	public List<String> GetQueryAsList(EDBM dbm, String field) {
		try {
			if (IsQuery(dbm, field)) {
				String sql = GetQuery(dbm, field);
				Set<String> uniqueAliases = new LinkedHashSet<>();
				try (BufferedReader reader = new BufferedReader(new StringReader(sql))) {
					String line;
					while ((line = reader.readLine()) != null) {
						String[] tokens = line.toUpperCase().split("\\s+AS\\s+");
						if (tokens.length > 1) {
							String alias = tokens[1].trim();
							alias = alias.split("\\s+")[0];
							alias = alias.replaceAll("[,()]", "");
							if (!alias.isEmpty() && alias.chars().noneMatch(Character::isWhitespace)) {
								uniqueAliases.add(alias);
							}
						}
					}
				} catch (IOException e) {
					UtilLog.e(getClass(), e);
				}
				UtilLog.i(getClass(), uniqueAliases.toString());
				return new ArrayList<>(uniqueAliases);
			}
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
		}
		return Collections.emptyList();
	}

	/********************************************************************
	 * IsExist
	 ********************************************************************/
	public boolean IsExist(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.IS, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "IsExist=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.IS, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.IS, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			return dbm.IsExist(edbSQL);	
		} catch (EDBException edbE) {
			throw edbE;
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::IsExist()} " + e.getMessage());
		}
	}	
	
	/********************************************************************
	 * Select
	 ********************************************************************/
	public boolean Select(EDBM dbm, String field, Object obj, List<Object> retList, Class<?> cls) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.SELECT, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "Select=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.SELECT, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.SELECT, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			return dbm.Select(this, edbSQL, retList, cls);
		} catch (EDBException edbE) {
			throw edbE;
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Select()} " + e.getMessage());
		}
	}		
	public List<Object> Select(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<Object> resultList = new ArrayList<Object>();
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.SELECT, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return null;
			}
			UtilLog.t(getClass(), "Select=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.SELECT, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.SELECT, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			if(dbm.Select(this, edbSQL, resultList, DataDB2.class)) {
				return resultList;
			}
			return null;
		} catch (EDBException edbE) {
			throw edbE;
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Select()} " + e.getMessage());
		}
	}
	
	public List<Object> Select(EDBM dbm, String field, String sql) throws Exception {
		try {
			List<Object> resultList = new ArrayList<Object>();
			EDBSql edbSQL = new EDBSql();
			edbSQL.setSql(sql);
			if (UtilString.isEmpty(edbSQL.getSql())) {
				return null;
			}
			UtilLog.t(getClass(), "Select=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.SELECT, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.SELECT, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			if(dbm.Select(this, edbSQL, resultList, DataDB2.class)) {
				return resultList;
			}
			return null;
		} catch (EDBException edbE) {
			throw edbE;
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Select()} " + e.getMessage());
		}
	}
	
	/********************************************************************
	 * Delete
	 ********************************************************************/
	public boolean Delete(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.DELETE, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "Delete=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.DELETE, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.DELETE, field) + "\n " + GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Delete()} " + e.getMessage());
		} 
	}
	
	/********************************************************************
	 * Delete
	 ********************************************************************/
	public boolean Delete(EDBM dbm, String field, String key, String value) throws Exception {
		try {
			String sql = this.GetQueryFormatExpand(field, key, value);
			if(UtilString.isEmpty(sql) ) {
				return false;
			}
			return dbm.Execute(sql, 0);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Delete()} " + e.getMessage());
		} 
	}
	
	/********************************************************************
	 * Insert
	 ********************************************************************/
	public boolean Insert(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.INSERT, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "Insert=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.INSERT, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.INSERT, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Insert()} " + e.getMessage());
		} 
	}
	
	/********************************************************************
	 * Update
	 ********************************************************************/
	public boolean Update(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.UPDATE, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "Update=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.UPDATE, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.UPDATE, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Update()} " + e.getMessage());
		} 
	}
	
	/********************************************************************
	 * Merge
	 ********************************************************************/
	public boolean Merge(EDBM dbm, String field, Object obj) throws Exception {
		try {
			List<EDBResult> list = DataList(obj);
			EDBSql edbSQL = GetQueryExpandBind(EDBColumn.MERGE, field, list);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
				return false;
			}
			UtilLog.t(getClass(), "Merge=" + Objects.requireNonNull(edbSQL).getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
					+ TIMEOUT(EDBColumn.MERGE, field));
			UtilLog.t(getClass(), "TimeOut=" + TIMEOUT(EDBColumn.MERGE, field) + "\n\t\t" + GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Merge()} " + e.getMessage());
		} 
	}
	
	/********************************************************************
	 * Execute
	 ********************************************************************/
	public boolean Execute(EDBM dbm, String sql) throws Exception {
		try {
			EDBSql edbSQL = new EDBSql(sql);
			UtilLog.t(getClass(), GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Execute()} " + e.getMessage());
		} 
	}	
	
	/********************************************************************
	 * SetTransaction
	 ********************************************************************/
	public void SetTransaction(EDBM dbm, boolean start) {
		try {
			dbm.SetTransaction(start);
		} catch(Exception e) {
			UtilLog.e(getClass(), "SetTransaction() " + e.getMessage());
		}
	}
	
	/********************************************************************
	 * Rollback
	 ********************************************************************/
	public void Rollback(EDBM dbm) {
		try {
			dbm.Rollback();
		} catch(Exception e) {
			UtilLog.e(getClass(), "Rollback() " + e.getMessage());
		}
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean LoadData(EDBM dbm, ResultSet rs, Object param) {
		try { 
			if(!(param instanceof ArrayList)) { return false; }
            List<DataDB2> list = (ArrayList<DataDB2>) param;
            ResultSetMetaData rsm = rs.getMetaData();
            while(rs.next()) {
                DataDB2 data = new DataDB2();
                data.LoadData(rsm, rs);
                list.add(data);
            }
            return true;
        } catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean LoadData(EDBM dbm, ResultSet rs, Object param, Class<?> cls) {
		try { 
			if(!(param instanceof ArrayList)) { return false; }
            if (cls == DataDB2.class) {
                List<DataDB2> list = (ArrayList<DataDB2>) param;
                ResultSetMetaData rsm = rs.getMetaData();
                while (rs.next()) {
                    DataDB2 data = new DataDB2();
                    data.LoadData(rsm, rs);
                    list.add(data);
                }
            }
            return true;
        } catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}	
}