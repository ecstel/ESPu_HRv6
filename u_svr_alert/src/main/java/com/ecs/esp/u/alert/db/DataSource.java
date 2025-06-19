package com.ecs.esp.u.alert.db;

import com.ecs.base.comm.UtilCalendar;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.EDBPostgres;
import com.ecs.base.db2.data.*;
import com.ecs.base.db2.exception.EDBException;
import com.ecs.esp.u.alert.define.Conf;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringReader;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.util.*;

public class DataSource extends EDBExpandData {
	/********************************************************************
	 * Final
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
			InitCreate(dbm);
			return true;
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}

	/********************************************************************
	 * CompareData
	 ********************************************************************
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
	 *
	 ********************************************************************/
	protected boolean CompareData(
			List<? extends Object> targetList, 		//	비교대상
	    	List<? extends Object> dbResultList,	//	DB질의결과(현재의값)
	    	List<String> compareList, 				//	DB에서 비교할 항목
	    	List<Object> insertList , List<Object> updateList, List<Object> deleteList)	//	결과를 담을 객체
	{
		try { 

			for(Object data1 : targetList) {				//	TARGET
				EDBResult tData = (EDBResult)data1;
				boolean isExist = false;
				for(Object  data : dbResultList) {			//	DATABASE
					if(data instanceof EDBResult) {
		    			EDBResult dData = (EDBResult)data;
		    			int equalsLen  = 0;
		    			int compareLen = compareList.size();
		    			for( int i = 0; i < compareLen; i++) {
		    				String t = dData.GetData(compareList.get(i));
		    				String d = tData.GetData(compareList.get(i));
		    				if ( !UtilString.isEmpty(t) && !UtilString.isEmpty(d) && !t.equals(d)) {
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
				if(isExist == false) {
					insertList.add( tData );
					UtilLog.t(getClass(), "INSERT="+tData.GetMap().toString());
				} 
			}
			for(Object  data : dbResultList) {				//	DATABASE
	    		boolean isExist = false;
	    		if(data instanceof EDBResult) {
	    			EDBResult dData = (EDBResult)data;    				
	    			for(Object data1 : targetList) {		//	TARGET
	    				int equalsLen  = 0;
		    			int compareLen = compareList.size();
		    			EDBResult tData = (EDBResult)data1;
		    			for( int i = 0; i < compareLen; i++) {
		    			//	UtilLog.t(getClass(), "t = > " + dData.toMapString());
		    				String t = dData.GetData(compareList.get(i));
		    				String d = tData.GetData(compareList.get(i));
		    			//	UtilLog.i(getClass(), "t="+t+", "+compareList.get(i));
		    			//	UtilLog.i(getClass(), "d="+d+", "+compareList.get(i));
		    			//	[09-01 09:50:19] INFO - [          DataSource]### t=null, SITE
		    			//	[09-01 09:50:19] INFO - [          DataSource]### d=NTS, SITE
		    			//	[09-01 09:50:19]ERROR - [          DataSource]### 
		    				if(!t.equals(d)) {
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
				CreateFunction(dbm, func, null);
			}
			for(String view : GetViewFirst()) {
				CreateView(dbm, view, null);
			}
			//	기본값 등록
			if (Conf.getInstance().START_TABLE_DAY() < (365*3)+1 && Conf.getInstance().END_TABLE_DAY() < (365*3)+1) {		
				List<String> dayList = UtilCalendar.getDateList(startDay, endDay);
				for(String name : GetBasic()) {
					InsertBasic(dbm, name, dateList, dayList);
				}
			} else {
				for(String name : GetBasic()) {
					InsertBasic(dbm, name, dateList, null);
				}
			}
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return true;
	}
	
	/********************************************************************
	 * CreateTable
	 ********************************************************************/
	private boolean CreateTable(EDBM dbm, List<String> dateList,  List<String> dayList) {
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
			return true;
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	public boolean CreateTable(EDBM dbm, String tableName, String sql) {
		try {
			return CreateTableSql(dbm, tableName, sql);
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}	

	/**********************************************************************************************************
	 * CreateTable : 기본 테이블 자동 생성
	 * @param dbm
	 * @param tableName
	 * @param cellList
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
			CreateTable(dbm, tableName, sql);		
			return true;
		} catch(Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	
	/********************************************************************
	 * InsertBasic
	 ********************************************************************/
	public boolean InsertBasic(EDBM dbm, String name, List<String> dateList, List<String> dayList) {
		try {
			if(dateList != null) {
				for(String date : dateList) {
					InsertBasic(dbm, name, EDBColumn.YYYYMM, date);
				}
			}
			if(dayList != null) {
				for(String date : dayList) {
					InsertBasic(dbm, name, EDBColumn.YYYYMMDD, date);
				}
			}
			return true;
		} catch(Exception e) {}
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
				if(IsExistFunction(dbm, name, null, null) == false) {
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
		} catch(Exception e) { }
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
		} catch (Exception e) {}
		return false;
	}
	
	public String GetQuery(EDBM dbm, String field) {
		try {
			String data = GetQuery(field);
			if(!UtilString.isEmpty(data)) {
				return data;
			}
		} catch (Exception e) {}
		return "";
	}
	
	protected List<String> GetQueryAsList(EDBM dbm, String field) {
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
							if (alias != null && !alias.isEmpty() && alias.chars().noneMatch(Character::isWhitespace)) {
								uniqueAliases.add(alias);
							}
						}
					}
				} catch (IOException e) { e.printStackTrace(); }
				UtilLog.i(getClass(), uniqueAliases.toString());
				return new ArrayList<>(uniqueAliases);
			}
		} catch (Exception e) { e.printStackTrace();}
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
			UtilLog.t(getClass(), "IsExist=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
			UtilLog.t(getClass(), "Select=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
	
	public List<Object> Select(EDBM dbm, String field, String sql) throws Exception {
		try {
			List<Object> resultList = new ArrayList<Object>();
			EDBSql edbSQL = new EDBSql();
			edbSQL.setSql(sql);
			if (edbSQL!=null && UtilString.isEmpty(edbSQL.getSql())) {
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
			UtilLog.t(getClass(), "Delete=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
			UtilLog.t(getClass(), "Insert=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
			UtilLog.t(getClass(), "Update=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
			UtilLog.t(getClass(), "Merge=" + edbSQL.getSql() + ", RowCount=" + edbSQL.getColList().size() + ", TimeOut="
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
			UtilLog.i(getClass(), GetDebugSQLFirst(edbSQL));
			return dbm.Execute(edbSQL);
		} catch(Exception e) {
			throw new Exception("{"+getClass().getSimpleName()+"::Execute()} " + e.getMessage());
		} 
	}	
	
	/********************************************************************
	 * SetTransaction
	 ********************************************************************/
	public boolean SetTransaction(EDBM dbm, boolean start) {
		try {
			dbm.SetTransaction(start);
			return true;
		} catch(Exception e) {}
		return false;
	}
	
	/********************************************************************
	 * Rollback
	 ********************************************************************/
	public boolean Rollback(EDBM dbm) {
		try {
			dbm.Rollback();
			return true;
		} catch(Exception e) {}
		return false;
	}
	
	@SuppressWarnings("unchecked")
	@Override
	public boolean LoadData(EDBM dbm, ResultSet rs, Object param) {
		try { 
			if(param instanceof ArrayList == false) { return false; }
			if(param instanceof List<?>) {
				List<DataDB2> list = (ArrayList<DataDB2>)param;
				ResultSetMetaData rsm = rs.getMetaData();
				while(rs.next()) {
					DataDB2 data = new DataDB2();
					data.LoadData(rsm, rs);
					list.add(data);
				}
				return true;
			}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}
	@SuppressWarnings("unchecked")
	@Override
	public boolean LoadData(EDBM dbm, ResultSet rs, Object param, Class<?> cls) {
		try { 
			if(param instanceof ArrayList == false) { return false; }
			if(param instanceof List<?>) {
				if(cls == DataDB2.class) {
					List<DataDB2> list = (ArrayList<DataDB2>)param;
					ResultSetMetaData rsm = rs.getMetaData();
					while(rs.next()) {
						DataDB2 data = new DataDB2();
						data.LoadData(rsm, rs);
						list.add(data);
					}
				}
				return true;
			}
		} catch (Exception e) { UtilLog.e(getClass(), e); }
		return false;
	}	
}