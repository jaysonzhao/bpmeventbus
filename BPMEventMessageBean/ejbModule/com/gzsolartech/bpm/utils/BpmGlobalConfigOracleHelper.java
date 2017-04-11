package com.gzsolartech.bpm.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;

public class BpmGlobalConfigOracleHelper implements IBpmGlobalConfigHelper {
	private final String findFirstCfgSql="select * from BPM_GLOBAL_CONFIG where CONFIG_STATUS='on'";
	@Override
	public Map<String, Object> getFirstActConfig() {
		Map<String, Object> dataMap=new HashMap<String, Object>();
		OracleJdbcUtils jdbcUtils=null;
		Connection conn=null;
		Statement st=null;
		ResultSet rs=null;
		try {
			jdbcUtils=new OracleJdbcUtils();
			conn=jdbcUtils.getConnection();
			st=conn.createStatement();
			rs=st.executeQuery(findFirstCfgSql);
			//只取第一条BPM配置信息
			if (rs.next()) {
				ResultSetMetaData rsMeta=rs.getMetaData();
				int columnCount=rsMeta.getColumnCount();
				for (int i=1; i<=columnCount; i++) {
					dataMap.put(rsMeta.getColumnLabel(i), rs.getObject(i));
				}
			}
		} catch (Exception ex) {
			System.out.println("获取BPM配置信息失败！");
			ex.printStackTrace();
		} finally {
			try {
				if (rs!=null) {
					rs.close();
				}
				if (st!=null) {
					st.close();
				}
			} catch (Exception ex) {
				System.out.println("释放数据库资源时发生异常！");
				ex.printStackTrace();
			}
			jdbcUtils.close(conn);
		}
		return dataMap;
	}

}
