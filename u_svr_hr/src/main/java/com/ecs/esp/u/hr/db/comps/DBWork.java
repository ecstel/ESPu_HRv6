package com.ecs.esp.u.hr.db.comps;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.ecs.base.comm.UtilFile;
import com.ecs.base.comm.UtilString;
import com.ecs.base.comm.log.UtilLog;
import com.ecs.base.db2.EDBM;
import com.ecs.base.db2.data.DataDB2;
import com.ecs.base.db2.data.EDBResult;
import com.ecs.base.secure.ECSSecure;
import com.ecs.base.secure.filescrty.FileScrty;
import com.ecs.esp.u.com.define.CommonConstants;
import com.ecs.esp.u.hr.db.data.DataHR;
import com.ecs.esp.u.hr.define.Conf;
import com.ecs.esp.u.hr.define.Define;

public class DBWork {
	public static int MAX_COUNT	=	30;
	
	private DataSource dbData;
	/*****************************************************************************************
	 * Constructor
	 *****************************************************************************************/
	public DBWork(DataSource dbData) {
		this.dbData = dbData;
	}

	/*****************************************************************************************
	 * dbSite : 사용자정보활용  SITE를 Key로해서 값은 SITE_NM 이 입력된 MAP을 통해서 중복 제거 후, Merge
	 *****************************************************************************************/
	public List<DataDB2> dbSite(EDBM dbm, List<Object> resultList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_SITE)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_SITE, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_SITE)) {
				Map<String, String> insertMap = new HashMap<String, String>();	//	key : SITE, value : SITE_NM
				for (Object obj : resultList) {
					EDBResult data = (EDBResult)obj;
					if(insertMap.get( data.GetData(CommonConstants.VAR_SITE) ) == null ) {
						insertMap.put(data.GetData(CommonConstants.VAR_SITE), data.GetData(CommonConstants.VAR_SITE_NM));
					}
				}				
				List<DataDB2> insert = new ArrayList<DataDB2>();
				for( String key : insertMap.keySet() ) {
					DataDB2 newData = new DataDB2();
					newData.SetData(CommonConstants.VAR_SITE,    	key);
			        newData.SetData(CommonConstants.VAR_SITE_NM,  insertMap.get(key));
			        insert.add(newData);
				}				
				if(dbData.Merge(dbm, CommonConstants.FD_MERGE_SITE, insert)){
					UtilLog.t(getClass(), "Merge success");
				}
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbSite()} " + e.getMessage());}
		return null;
	}
	
	/*****************************************************************************************
	 * dbTenant : 사용자정보활용  "SITE | TENANT"를 Key로해서 중복제거 후, Merge
	 *****************************************************************************************/
	public List<DataDB2> dbTenant(EDBM dbm, List<Object> userList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_TENANT)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_TENANT, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_TENANT)) {
				Map<String, DataDB2> insertMap = new HashMap<String, DataDB2>();	//	key : SITE | TENANT, value : DataDB2
				for (Object obj : userList) {
					EDBResult data  = (EDBResult)obj;
					String site     = data.GetData(CommonConstants.VAR_SITE);
					String tenant   = data.GetData(CommonConstants.VAR_TENANT);
					String siteNM   = data.GetData(CommonConstants.VAR_SITE_NM);
					String tenantNM = data.GetData(CommonConstants.VAR_TENANT_NM);
					if(UtilString.isEmpty(site)) 	 { continue; }
					if(UtilString.isEmpty(tenant)) 	 { continue; }
					if(UtilString.isEmpty(tenantNM)) { continue; }
					if(!insertMap.containsKey( site + Conf.getInstance().SEPARATOR() + tenant )) {
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_SITE, site) ;
						newData.SetData(CommonConstants.VAR_TENANT, tenant);
						newData.SetData(CommonConstants.VAR_SITE_NM, siteNM) ;
						newData.SetData(CommonConstants.VAR_TENANT_NM, tenantNM);
						insertMap.put(site + Conf.getInstance().SEPARATOR() + tenant, newData);
					}
				}
				List<DataDB2> insert = new ArrayList<DataDB2>();
			    for( String key : insertMap.keySet() ){
			    	insert.add( insertMap.get(key));
			    }			    
				dbData.Merge(dbm, CommonConstants.FD_MERGE_TENANT, insert);
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbTenant()} " + e.getMessage());}
		return null;
	}
	
	/*****************************************************************************************
	 * dbDivision : 과정보  "SITE | TENANT | DIVISION"를 Key로해서 중복제거 후, Merge
	 * 2025.05 현재 국세청에서만 사용
	 *****************************************************************************************/
	public List<DataDB2> dbDivision(EDBM dbm, List<Object> userList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_DIVISION)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_DIVISION, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_DIVISION)) {
				Map<String, DataDB2> insertMap = new HashMap<String, DataDB2>();	//	key : SITE | TENANT | DIVISION, value : DataDB2
				for (Object obj : userList) {
					EDBResult data  	= (EDBResult)obj;
					String site     	= data.GetData(CommonConstants.VAR_SITE);
					String siteNM      	= data.GetData(CommonConstants.VAR_SITE_NM);
					String tenant   	= data.GetData(CommonConstants.VAR_TENANT);
					String tenantNM   	= data.GetData(CommonConstants.VAR_TENANT_NM);
					String division 	= data.GetData(CommonConstants.VAR_DIVISION);
					String divisionNM   = data.GetData(CommonConstants.VAR_DIVISION_NM);
					if(UtilString.isEmpty(site)) 	 	{ continue; }
					if(UtilString.isEmpty(siteNM)) 	 	{ continue; }
					if(UtilString.isEmpty(tenant)) 	 	{ continue; }
					if(UtilString.isEmpty(tenantNM)) 	{ continue; }
					if(UtilString.isEmpty(division)) 	{ continue; }
					if(UtilString.isEmpty(divisionNM)) 	{ continue; }
					if(!insertMap.containsKey( site + Conf.getInstance().SEPARATOR() + tenant + Conf.getInstance().SEPARATOR() + division )) {
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_SITE, site) ;
						newData.SetData(CommonConstants.VAR_TENANT, tenant);
						newData.SetData(CommonConstants.VAR_SITE_NM, siteNM) ;
						newData.SetData(CommonConstants.VAR_TENANT_NM, tenantNM);
						newData.SetData(CommonConstants.VAR_DIVISION, division) ;
						newData.SetData(CommonConstants.VAR_DIVISION_NM, divisionNM);
						insertMap.put(site + Conf.getInstance().SEPARATOR() + tenant + Conf.getInstance().SEPARATOR() + division, newData);
					}
				}
				List<DataDB2> insert = new ArrayList<DataDB2>();
			    for( String key : insertMap.keySet() ){
			    	insert.add( insertMap.get(key));
			    }			    
				dbData.Merge(dbm, CommonConstants.FD_MERGE_DIVISION, insert);
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbDivision()} " + e.getMessage());}
		return null;
	}
	
	/*****************************************************************************************
	 * dbTeam : 팀정보  "SITE | TENANT | DIVISION | TEAM"를 Key로해서 중복제거 후, Merge
	 * 2025.05 현재 국세청에서만 사용
	 *****************************************************************************************/
	public List<DataDB2> dbTeam(EDBM dbm, List<Object> userList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_TEAM)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_TEAM, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_TEAM)) {
				Map<String, DataDB2> insertMap = new HashMap<String, DataDB2>();	//	key : SITE | TENANT | DIVISION | TEAM, value : DataDB2
				for (Object obj : userList) {
					EDBResult data  	= (EDBResult)obj;
					String site     	= data.GetData(CommonConstants.VAR_SITE);
					String siteNM      	= data.GetData(CommonConstants.VAR_SITE_NM);
					String tenant   	= data.GetData(CommonConstants.VAR_TENANT);
					String tenantNM   	= data.GetData(CommonConstants.VAR_TENANT_NM);
					String division 	= data.GetData(CommonConstants.VAR_DIVISION);
					String divisionNM   = data.GetData(CommonConstants.VAR_DIVISION_NM);
					String team		 	= data.GetData(CommonConstants.VAR_TEAM);
					String teamNM	    = data.GetData(CommonConstants.VAR_TEAM_NM);
					if(UtilString.isEmpty(site)) 	 	{ continue; }
					if(UtilString.isEmpty(siteNM)) 	 	{ continue; }
					if(UtilString.isEmpty(tenant)) 	 	{ continue; }
					if(UtilString.isEmpty(tenantNM)) 	{ continue; }
					if(UtilString.isEmpty(division)) 	{ continue; }
					if(UtilString.isEmpty(divisionNM)) 	{ continue; }
					if(UtilString.isEmpty(team)) 		{ continue; }
					if(UtilString.isEmpty(teamNM)) 		{ continue; }
					
					if(!insertMap.containsKey( site + Conf.getInstance().SEPARATOR() + tenant + Conf.getInstance().SEPARATOR() + division  + Conf.getInstance().SEPARATOR() + team)) {
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_SITE, site) ;
						newData.SetData(CommonConstants.VAR_TENANT, tenant);
						newData.SetData(CommonConstants.VAR_SITE_NM, siteNM) ;
						newData.SetData(CommonConstants.VAR_TENANT_NM, tenantNM);
						newData.SetData(CommonConstants.VAR_DIVISION, division) ;
						newData.SetData(CommonConstants.VAR_DIVISION_NM, divisionNM);
						newData.SetData(CommonConstants.VAR_TEAM, team) ;
						newData.SetData(CommonConstants.VAR_TEAM_NM, teamNM);
						insertMap.put(site + Conf.getInstance().SEPARATOR() + tenant + Conf.getInstance().SEPARATOR() + division + Conf.getInstance().SEPARATOR() + team, newData);
					}
				}
				List<DataDB2> insert = new ArrayList<DataDB2>();
			    for( String key : insertMap.keySet() ){
			    	insert.add( insertMap.get(key));
			    }			
				dbData.Merge(dbm, CommonConstants.FD_MERGE_TEAM, insert);
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbTeam()} " + e.getMessage());}
		return null;
	}
	
	public int dbDept(EDBM dbm, List<Object> userOrDeptList, String method) throws Exception {
		int result = -1;
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_DEPT)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_DEPT, new DataDB2());
			}
			if(!dbData.IsQuery(dbm, CommonConstants.FD_GET_CURRENT_DEPT)) {
				return 0;	//	부서 설정을 하지 않는 경우, 성공 처리를 위해서 0 리턴함.
			}
			List<Object> dbList = dbData.Select(dbm, CommonConstants.FD_GET_CURRENT_DEPT, new DataDB2());
			
			List<Object> insertList = new ArrayList<>();
			List<Object> updateList = new ArrayList<>();
			List<Object> deleteList = new ArrayList<>();
			
			List<String> compareList = null;
			
			if(Define.MULTIPLE_QUERY.equals(method)) {
				compareList = Arrays.asList(CommonConstants.VAR_SITE, CommonConstants.VAR_TENANT, CommonConstants.VAR_DEPT_CODE);			//	다수 질의
			} else {
				compareList = Arrays.asList(CommonConstants.VAR_SITE, CommonConstants.VAR_TENANT, CommonConstants.VAR_NAME_TREE);			//	단수 질의
			}
			Set<String> hashSet = new HashSet<>();

			if (dbData.CompareData(userOrDeptList, dbList, compareList, insertList, updateList, deleteList)) {
				UtilLog.i(getClass(), String.format(
						"{dbDept} deptList = %d dbList = %d insertList = %d, updateList = %d, deleteList = %d",
						userOrDeptList.size(), dbList.size(), insertList.size(), updateList.size(), deleteList.size()));

				if (insertList.isEmpty() && updateList.isEmpty()) {
					return 0;
				}
				processInserts(dbm, insertList, hashSet, method);
				processUpdates(dbm, updateList,  method);
				result = insertList.size() + updateList.size() + deleteList.size();
			}
		} catch (Exception e) {
			throw new Exception("{" + getClass().getSimpleName() + "::dbDept()} " + e.getMessage(), e);
		}
		return result;
	}
	
	/**
	 * processInserts
	 *
	 * @param dbm 객체
	 * @param insertList 입력데이터
	 * @param hashSet hashSet
	 * @throws Exception 예외처리
	 */
	private void processInserts(EDBM dbm, List<Object> insertList, Set<String> hashSet, String method) throws Exception {
		for (Object obj : insertList) {
			DataDB2 data 		= (DataDB2) obj;
			String site 		= data.GetData(CommonConstants.VAR_SITE);
			String tenant 		= data.GetData(CommonConstants.VAR_TENANT);
			String nametree 	= data.GetData(CommonConstants.VAR_NAME_TREE);
			String codetree 	= data.GetData(CommonConstants.VAR_CODE_TREE);
			String dept_code 	= data.GetData(CommonConstants.VAR_DEPT_CODE);		//	사이트에서 사용하는 부서 코드
			String branch_code	= data.GetData(CommonConstants.VAR_BRANCH_CODE);		//	점번호
			String abbrNM 		= data.GetData(CommonConstants.VAR_ABBR_NM);
			String dept_parent_code	= data.GetData(CommonConstants.VAR_DEPT_PARENT_CODE);
			
			logInsert(nametree, codetree, dept_code, branch_code);
			
			String[] nameTreeSplit = null;
			String[] codeTreeSplit = null;
			if(Define.MULTIPLE_QUERY.equals(method)) {
				if (UtilString.isEmpty(site) || UtilString.isEmpty(tenant) || UtilString.isEmpty(nametree) || UtilString.isEmpty(codetree)) {
					continue;
				}
				nameTreeSplit = nametree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
				codeTreeSplit = codetree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
			} else {
				if (UtilString.isEmpty(site) || UtilString.isEmpty(tenant) || UtilString.isEmpty(nametree)) {
					continue;
				}
				nameTreeSplit = nametree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
			}
			int depth     = 0;
			int parent_id = -1;
			DataHR hrData = new DataHR();
			for (String deptNM : nameTreeSplit) {
				if (UtilString.isEmpty(deptNM)) {
					continue;
				}
				depth++;
				populateInputData(hrData, site, tenant, codetree, deptNM, depth, dept_parent_code, parent_id, branch_code, abbrNM);				
				if(Define.MULTIPLE_QUERY.equals(method)) {
					logDepth(depth, deptNM, codeTreeSplit[depth-1], method);		
				} else {
					logDepth(depth, deptNM, nameTreeSplit[depth-1], method);	
				}
				parent_id = handleInsert(dbm, hrData, hashSet);
			}
		}
	}
	
	private void populateInputData(DataHR hrData, String site, String tenant, String codetree, String deptNM, int depth, String dept_parent_code, int parent_id, String branch_code, String abbrNM) {
		if (depth == 1) {
			hrData.name_tree = deptNM;
		} else {
			hrData.name_tree += Conf.getInstance().SEPARATOR() + deptNM;
		}
		String dept_code    	= UtilString.Split(codetree, Conf.getInstance().SEPARATOR(), (depth - 1));
		hrData.site 			= site;
		hrData.tenant 			= tenant;
		hrData.dept_code 		= dept_code;
		hrData.dept_parent_code = dept_parent_code;
		hrData.dept_nm 			= deptNM;
		hrData.depth 			= depth;
		hrData.parent_id 		= parent_id;
		hrData.branch_code   	= branch_code;
		hrData.abbr_nm			= abbrNM;
	}
	private int handleInsert(EDBM dbm, DataHR hrData, Set<String> hashSet) throws Exception {
		List<Object> deptList = dbData.Select(dbm, CommonConstants.FD_GET_DEPT, hrData);
		int parent_id = -1;
		if (!deptList.isEmpty()) {
			for (Object insDeptDataObj : deptList) {
				DataDB2 insDeptData = (DataDB2) insDeptDataObj;
				if (!UtilString.isEmpty(insDeptData.GetData(CommonConstants.VAR_DEPT_ID))) {
					parent_id = Integer.parseInt(insDeptData.GetData(CommonConstants.VAR_DEPT_ID));
					hashSet.add(String.valueOf(parent_id));
				}
			}
		} else {
			logInsertData(hrData);
			dbData.Insert(dbm, CommonConstants.FD_INSERT_DEPT, hrData);
			List<Object> insDeptList = dbData.Select(dbm, CommonConstants.FD_GET_DEPT, hrData);
			for (Object insDeptDataObj : insDeptList) {
				DataDB2 insDeptData = (DataDB2) insDeptDataObj;
				if (!UtilString.isEmpty(insDeptData.GetData(CommonConstants.VAR_DEPT_ID))) {
					parent_id = Integer.parseInt(insDeptData.GetData(CommonConstants.VAR_DEPT_ID));
					hashSet.add(String.valueOf(parent_id));
				}
			}
		}
		return parent_id;
	}	
	private void logDepth(int depth, String deptNM, String deptCode2NM, String method) {
		if(Define.MULTIPLE_QUERY.equals(method)) {
			UtilLog.i(getClass(), String.format("# %s[ %d ] %s[%s] %s[%s] ", CommonConstants.VAR_DEPTH, depth, CommonConstants.VAR_DEPT_NM, deptNM, CommonConstants.VAR_DEPT_CODE, deptCode2NM));
		} else {
			UtilLog.i(getClass(), String.format("# %s[ %d ] %s[%s] %s[%s] ", CommonConstants.VAR_DEPTH, depth, CommonConstants.VAR_DEPT_NM, deptNM, CommonConstants.VAR_DEPT_NM,   deptCode2NM));
		}
	}
	private void logInsert(String nameTree, String codeTree, String deptCode, String branchCode) {
		UtilLog.i(getClass(), "");
		UtilLog.i(getClass(), "##############################################################################################################################################################");
		StringBuilder updateLog = new StringBuilder("# (INSERT) nameTree=" + nameTree);
		if (codeTree != null) {
			updateLog.append(", codeTree=").append(codeTree);
		}
		if (deptCode != null) {
			updateLog.append(", deptCode=").append(deptCode);
		}
		if (branchCode != null) {
			updateLog.append(", branchCode=").append(branchCode);
		}
		UtilLog.i(getClass(), updateLog.toString());
		UtilLog.i(getClass(), "##############################################################################################################################################################");
	}

	private void logInsertData(DataHR hrData) {
		StringBuilder log = new StringBuilder();
		if (hrData.parent_id > 0) {
			log.append(String.format("[%4s] ", Define.TREE));
		} else {
			log.append(String.format("[%4s] ", Define.ROOT));
		}
		log.append(String.format("%s=%s, %s=%s, %s=%s, %s=%s, %s=%d, %s=%d",
				CommonConstants.VAR_SITE,
				hrData.site,
				CommonConstants.VAR_TENANT,
				hrData.tenant,
				CommonConstants.VAR_DEPT_NM,
				hrData.dept_nm,
				CommonConstants.VAR_NAME_TREE,
				hrData.name_tree,
				CommonConstants.VAR_PARENT_ID,
				hrData.parent_id,
				CommonConstants.VAR_DEPTH,
				hrData.depth));
		if (hrData.dept_code != null) {
			log.append(String.format(", %s=%s", CommonConstants.VAR_DEPT_CODE, hrData.dept_code));
		}
		if (hrData.branch_code != null) {
			log.append(String.format(", %s=%s", CommonConstants.VAR_BRANCH_CODE, hrData.branch_code));
		}
		UtilLog.i(getClass(), "[INSERT] " + log.toString());
	}

	private void processUpdates(EDBM dbm, List<Object> updateList, String method) throws Exception {
		for (Object obj : updateList) {
			DataDB2 data 		= (DataDB2) obj;
			String site 		= data.GetData(CommonConstants.VAR_SITE);
			String tenant 		= data.GetData(CommonConstants.VAR_TENANT);
			String nametree 	= data.GetData(CommonConstants.VAR_NAME_TREE);
			String codetree 	= data.GetData(CommonConstants.VAR_CODE_TREE);
			String deptCode 	= data.GetData(CommonConstants.VAR_DEPT_CODE);
			String branchCode   = data.GetData(CommonConstants.VAR_BRANCH_CODE);
			
			logUpdate(nametree, codetree, deptCode, branchCode);

			String[] nameTreeSplit = null;
			String[] codeTreeSplit = null;
			if(Define.MULTIPLE_QUERY.equals(method)) {
				if (UtilString.isEmpty(site) || UtilString.isEmpty(tenant) || UtilString.isEmpty(nametree) || UtilString.isEmpty(codetree)) {
					continue;
				}
				nameTreeSplit = nametree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
				codeTreeSplit = codetree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
			} else {
				if (UtilString.isEmpty(site) || UtilString.isEmpty(tenant) || UtilString.isEmpty(nametree)) { 
					continue;
				}
				nameTreeSplit = nametree.split(String.format("\\%s", Conf.getInstance().SEPARATOR()));
			}
			
			int depth     = 0;
			int parent_id = -1;
			DataHR hrData = new DataHR();
			for (String deptNM : nameTreeSplit) {
				if (UtilString.isEmpty(deptNM)) {
					continue;
				}
				depth++;
				populateUpdateData(hrData, site, tenant, codetree, deptNM, depth, parent_id, branchCode);				
				if(Define.MULTIPLE_QUERY.equals(method)) {
					logDepth(depth, deptNM, codeTreeSplit[depth-1], method);		
				} else {
					logDepth(depth, deptNM, nameTreeSplit[depth-1], method);
				}
				parent_id = handleUpdate(dbm, hrData);
			}
		}
	}

	/**
	 * 
	 * @param hrData 객체
	 * @param site 사이트영문명
	 * @param tenant 테넌트영문명
	 * @param codetree 부서코드 tree
	 * @param deptNM 부서명
	 * @param depth 깊이
	 * @param parent_id 부모ID
	 * @param branch_code 지점코드
	 */
	private void populateUpdateData(DataHR hrData, String site, String tenant, String codetree, String deptNM, int depth, int parent_id, String branch_code) {
		if (depth == 1) {
			hrData.name_tree = deptNM;
		} else {
			hrData.name_tree += Conf.getInstance().SEPARATOR() + deptNM;
		}
		String dept_code   	 	= UtilString.Split(codetree, Conf.getInstance().SEPARATOR(), (depth - 1));
		hrData.site 			= site;
		hrData.tenant 			= tenant;
		hrData.dept_code 		= dept_code;
		hrData.dept_nm 			= deptNM;
		hrData.depth 			= depth;
		hrData.parent_id 		= parent_id;
		hrData.branch_code   	= branch_code;
	}

	/**
	 * 부서 정보를 조회한 후, 부서 정보를 insert하고, 해당 부서 코드를 부모 코드로 리턴함
	 * 
	 *  
	 * @param dbm 객체
	 * @param hrData 부서 처리 객체
	 * @return parent_id(부모ID)로 이후 사용을 위해서 리턴
	 * @throws Exception 예외 처리
	 */
	private int handleUpdate(EDBM dbm, DataHR hrData) throws Exception {
		List<Object> deptList = dbData.Select(dbm, CommonConstants.FD_GET_DEPT, hrData);
		int parent_id = -1;
		if (!deptList.isEmpty()) {
			logUpdateData(hrData);
			dbData.Insert(dbm, CommonConstants.FD_UPDATE_DEPT, hrData);
			List<Object> insDeptList = dbData.Select(dbm, CommonConstants.FD_GET_DEPT, hrData);
			for (Object insDeptDataObj : insDeptList) {
				DataDB2 insDeptData = (DataDB2) insDeptDataObj;
				if (!UtilString.isEmpty(insDeptData.GetData(CommonConstants.VAR_DEPT_ID))) {
					parent_id = Integer.parseInt(insDeptData.GetData(CommonConstants.VAR_DEPT_ID));
				}
			}
		}
		return parent_id;
	}

	/**
	 * 로그갱신
	 * 
	 * @param nameTree 부서명tree "A‡B‡C"
	 * @param codeTree 부서코드tree "1‡2‡3"
	 * @param deptCode 사이트제공부서코드
	 * @param branchCode 사이트제공점코드
	 */
	private void logUpdate(String nameTree, String codeTree, String deptCode, String branchCode) {
		UtilLog.i(getClass(), "");
		UtilLog.i(getClass(), "##############################################################################################################################################################");
		StringBuilder updateLog = new StringBuilder("# (UPDATE) nameTree=" + nameTree);
		if (codeTree != null) {
			updateLog.append(", codeTree=").append(codeTree);
		}
		if (deptCode != null) {
			updateLog.append(", deptCode=").append(deptCode);
		}
		if (branchCode != null) {
			updateLog.append(", branchCode=").append(branchCode);
		}
		UtilLog.i(getClass(), updateLog.toString());
		UtilLog.i(getClass(), "##############################################################################################################################################################");
	}

	/**
	 * 로그갱신
	 * @param hrData 객체
	 */
	private void logUpdateData(DataHR hrData) {
		StringBuilder log = new StringBuilder();
		if (hrData.parent_id > 0) {
			log.append(String.format("[%4s] ", Define.TREE));
		} else {
			log.append(String.format("[%4s] ", Define.ROOT));
		}
		log.append(String.format("%s=%s, %s=%s, %s=%s, %s=%s, %s=%d, %s=%d",
				CommonConstants.VAR_SITE,
				hrData.site,
				CommonConstants.VAR_TENANT,
				hrData.tenant,
				CommonConstants.VAR_DEPT_NM,
				hrData.dept_nm,
				CommonConstants.VAR_NAME_TREE,
				hrData.name_tree,
				CommonConstants.VAR_PARENT_ID,
				hrData.parent_id,
				CommonConstants.VAR_DEPTH,
				hrData.depth));
		if (hrData.dept_code != null) {
			log.append(String.format(", %s=%s", CommonConstants.VAR_DEPT_CODE, hrData.dept_code));
		}
		if (hrData.branch_code != null) {
			log.append(String.format(", %s=%s", CommonConstants.VAR_BRANCH_CODE, hrData.branch_code));
		}
		UtilLog.i(getClass(), "[UPDATE] " + log.toString());
	}

	/**
	 *
	 * 직위정보 처리 함수
	 * "SITE | GRADE_NM" 를 Key로해서 중복제거 후, Merge
	 *
	 * @param dbm 객체
	 * @param userList 직위를 포함한 사용자 정보
	 * @return List<DataDB2>는 갱신된 데이터
	 * @throws Exception 예외 처리
	 */
	public List<DataDB2> dbGrade(EDBM dbm, List<Object> userList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_GRADE)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_GRADE, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_GRADE)) {
				Map<String, DataDB2> insertMap = new HashMap<String, DataDB2>();									//	Key : SITE | GRADE_NM, value : DataDB2
				for (Object obj : userList) {
					DataDB2 data     = (DataDB2)obj;
					String gradeNM   = data.GetData(CommonConstants.VAR_GRADE_NM);
					String site      = data.GetData(CommonConstants.VAR_SITE);
					String siteNM    = data.GetData(CommonConstants.VAR_SITE_NM);
					String gradeCode = data.GetData(CommonConstants.VAR_GRADE_CODE);
					if(UtilString.isEmpty(site)) 	{ continue; }
					if(UtilString.isEmpty(gradeNM)) { continue; }
					
					String key = site + Conf.getInstance().SEPARATOR() + gradeNM + Optional.ofNullable(gradeCode).orElse("");
					if(!insertMap.containsKey(key)) {
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_SITE,   	    site);
						newData.SetData(CommonConstants.VAR_SITE_NM, 	siteNM);
						newData.SetData(CommonConstants.VAR_GRADE_NM,    gradeNM);
						newData.SetData(CommonConstants.VAR_GRADE_CODE,  gradeCode);
						insertMap.put(key, newData);
					}
				}
				List<DataDB2> insert = new ArrayList<DataDB2>();
			    for( String key : insertMap.keySet() ){
			    	insert.add( insertMap.get(key) );
			    }
			    dbData.Merge(dbm, CommonConstants.FD_MERGE_GRADE, insert);
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbGrade()} " + e.getMessage());}
		return null;
	}
	
	/**
	 * 직책 정보 갱신
	 * 직책정보 "SITE | POSITION_NM" 를 Key로해서 중복제거 후, Merge
	 *
	 * @param dbm 객체
	 * @param userList 사용자(직책) 리스트
	 * @return  List<DataDB2> 갱신정보
	 * @throws Exception 예외처리
	 */
	public List<DataDB2> dbPosition(EDBM dbm, List<Object> userList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_POSITION)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_POSITION, new DataDB2());
			}
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_POSITION)) {
				Map<String, DataDB2> insertMap = new HashMap<String, DataDB2>();						//	Key : SITE | POSITION_NM, value : DataDB2
				for (Object obj : userList) {
					DataDB2 data     	= (DataDB2)obj;
					String positionNM   = data.GetData(CommonConstants.VAR_POSITION_NM);
					String site      	= data.GetData(CommonConstants.VAR_SITE);
					String siteNM    	= data.GetData(CommonConstants.VAR_SITE_NM);
					String positionCode = data.GetData(CommonConstants.VAR_POSITION_CODE);
					if(UtilString.isEmpty(site)) 		{ continue; }
					if(UtilString.isEmpty(positionNM)) 	{ continue; }
					
					String key = site + Conf.getInstance().SEPARATOR() + positionNM + Optional.ofNullable(positionCode).orElse("");
					if(!insertMap.containsKey( key )) {		//	SITE | POSITION_NM
						DataDB2 newData = new DataDB2();
						newData.SetData(CommonConstants.VAR_SITE,   	   	   site);
						newData.SetData(CommonConstants.VAR_SITE_NM, 	   siteNM);
						newData.SetData(CommonConstants.VAR_POSITION_NM,    positionNM);
						newData.SetData(CommonConstants.VAR_POSITION_CODE,  positionCode);
						insertMap.put(key, newData);	
					}
				}
				List<DataDB2> insert = new ArrayList<DataDB2>();
			    for( String key : insertMap.keySet() ){
			    	insert.add( insertMap.get(key) );
			    }
			    dbData.Merge(dbm, CommonConstants.FD_MERGE_POSITION, insert);
				return insert;
			}
		} catch(Exception e) { throw new Exception("{"+getClass().getSimpleName()+"::dbPosition()} " + e.getMessage());}
		return null;
	}

	/**
	 * 사용자 지원 서비스 설정 (ESP_UC.U_USER_SERVICE)
	 * 
	 * @param dbm 객체
	 * @param resultList 사용자정보 
	 * @throws Exception 예외처리
	 */
	public void dbUserService(EDBM dbm, List<Object> resultList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_MERGE_USER_SERVICE)) {
				for (Object obj : resultList) {
					EDBResult data = (EDBResult) obj;
					if( !UtilString.isEmpty(data.GetData(CommonConstants.VAR_MCID_FL))) {		// GET_SOURCE_HR_USER 에 MCID_FL이 존재할 경우, 전체 서비스가 존재한다고 보고 처리함.
						dbData.Merge(dbm, CommonConstants.FD_MERGE_USER_SERVICE, resultList);
						return;
					}
				}
			}
		} catch (Exception e) {
			throw new Exception("{" + getClass().getSimpleName() + "::dbUserService()} " + e.getMessage());
		}
	}

	/**
	 * 사용자 정보 갱신(ESP_CORE.U_USER)
	 *
	 * @param dbm 객체
	 * @param userInputList 사용자 정보
	 * @throws Exception  예외처리
	 */
	public void dbUser(EDBM dbm, List<Object> userInputList) throws Exception {
		try {
			if(dbData.IsQuery(dbm, CommonConstants.FD_DISABLE_USER)) {
				dbData.Update(dbm, CommonConstants.FD_DISABLE_USER, new DataDB2());
			}
			for (Object obj : userInputList) {
				EDBResult data = (EDBResult) obj;
				// 사진 정보 갱신 : U_USER 테이블에 필드가 존재할 경우에만 사용함.
				if (!UtilString.isEmpty(Conf.getInstance().PICTURE_PATH())) {
					String picDir = UtilFile.getPath(Conf.getInstance().PICTURE_PATH());
					String picField = UtilFile.getFileExpectExtension(Conf.getInstance().PICTURE_PATH());
					String picExt = UtilFile.getFileExtension(Conf.getInstance().PICTURE_PATH());
					if (!UtilString.isEmpty(picDir) && !UtilString.isEmpty(picField) && !UtilString.isEmpty(picExt)) {
						String pictureURL = picDir + File.separator + data.GetData(picField) + "." + picExt;
						if (UtilFile.exists(pictureURL)) {
							data.SetData(CommonConstants.VAR_PICTURE, data.GetData(picField) + "." + picExt);
						} else {
							data.SetData(CommonConstants.VAR_PICTURE, "");
						}
					} else {
						data.SetData(CommonConstants.VAR_PICTURE, "");
					}
				}

				// 암호화 정보 갱신
				for (String d : Conf.getInstance().CRYPTO_FIELD()) {
					String value = data.GetData(d);
					if (!UtilString.isEmpty(value)) {
						if (Conf.getInstance().CRYPTO() == Define.EncryptDB) {
							data.SetData(d, ECSSecure.EncryptDB(value));
						}
					}
				}
			}
			dbData.Merge(dbm, CommonConstants.FD_MERGE_USER, userInputList);
		} catch (Exception e) {
			throw new Exception("{" + getClass().getSimpleName() + "::dbUser()} " + e.getMessage());
		}
	}

	/**
	 * U_MEMBER 테이블 갱신(ESP_CORE.U_MEMBER)
	 *
	 * @param dbm 객체
	 * @param userInputList 사용자 정보
	 * @throws Exception 예외처리
	 */
	public void dbMember(EDBM dbm, List<Object> userInputList) throws Exception {
		try {
			for (Object obj : userInputList) {
				EDBResult data = (EDBResult) obj;
				String encField = Conf.getInstance().ENCRYPTION_FIELD();
				String value = data.GetData(encField);
				if (UtilString.isEmpty(value)) { // 얻어진 값이 없으면..
					value = encField;
				}
				if(Conf.getInstance().ENCRYPTION()  != Define.None) {
					if (Conf.getInstance().ENCRYPTION() == Define.FileScrtySSO) {
						String userID = data.GetData(CommonConstants.VAR_USER_ID);
						if(UtilString.isEmpty(userID)) {
							userID = data.GetData(CommonConstants.VAR_USER_NO);
						}
						UtilLog.t(getClass(), "FileScrtySSO encryptPassword_sso(pw:" + value + ", id:"
								+ userID + ")");
						data.SetData(CommonConstants.VAR_USER_PW, FileScrty.encryptPassword_sso(value, userID)); // data.GetData(CommonConstants.VAR_USER_ID)));
					} else if (Conf.getInstance().ENCRYPTION() == Define.EncryptDB) {
						data.SetData(CommonConstants.VAR_USER_PW, ECSSecure.EncryptDB(value));
					} else {
						data.SetData(CommonConstants.VAR_USER_PW, data.GetData(value));
					}
				} else {
					data.SetData(CommonConstants.VAR_USER_PW, value);
				}
			}
			dbData.Merge(dbm, CommonConstants.FD_MERGE_MEMBER, userInputList);
		} catch (Exception e) {
			UtilLog.e(getClass(), e);
			throw new Exception("{" + getClass().getSimpleName() + "::dbMember()} " + e.getMessage());
		}
	}

	/**
	 * U_USER 테이블에서 삭제처리된 사용자에 대해서 계정 잠금(ESP_CORE.U_MEMBER)
	 * - U_MEMBER 테이블의 MEMBER_STATE_CD의 상태를 DEL로 변경
	 *
	 * @param dbm 객체
	 * @throws Exception 예외처리
	 */
	public void delMemberState(EDBM dbm)  throws Exception {
		try {
			if (dbData.IsQuery(dbm, CommonConstants.FD_DEL_MEMBER_STATE)) {
				dbData.Update(dbm, CommonConstants.FD_DEL_MEMBER_STATE, new DataDB2());
			}
		} catch (Exception e) {
			throw new Exception("{" + getClass().getSimpleName() + "::delMemberState()} " + e.getMessage());
		}
	}

	/**
	 * U_MEMBER 테이블을 이용해서 U_USER의 LOGIN_FL값 갱신(ESP_CORE.U_USER)
	 * - U_USER테이블에서 U_MEMBER의 설정 여부 파악 처리 함수
	 *
	 * @param dbm 객체
	 * @throws Exception 예외처리
	 */
	public void updateUserLoginFL(EDBM dbm)  throws Exception {
		try {
			if (dbData.IsQuery(dbm, CommonConstants.FD_UPDATE_USER_LOGIN_FL)) {
				dbData.Update(dbm, CommonConstants.FD_UPDATE_USER_LOGIN_FL, new DataDB2());
			}
		} catch (Exception e) {
			throw new Exception("{" + getClass().getSimpleName() + "::updateUserLoginFL()} " + e.getMessage());
		}
	}
}