package com.gzsolartech.bpm.utils;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.sql.DataSource;

/**
 * Oracle JDBC 工具类
 * 
 * @author sujialin
 *
 */
public class OracleJdbcUtils {
	private static final String JNDI_NAME="jndi/smartforms";
	private static DataSource ds;
	private static Context initContext;
	private static final String TEST_SQL="select 1 from dual";
	
	static {
		try {
			initContext = new InitialContext();
			ds = (DataSource) initContext.lookup(JNDI_NAME);// JNDI
		} catch (Exception ex) {
			System.out.println("初始化JNDI连接失败！");
			ex.printStackTrace();
		}
	}
	
	/**
	 * 构造函数测试数据库连接是否有效，还用来激活JNDI连接池。
	 * 如果不首先激活JNDI连接池，第一次获取数据库连接会失败。
	 */
	public OracleJdbcUtils() {
		Connection conn = null;
		Statement st=null;
		ResultSet rs=null;
		try {
			conn = ds.getConnection();
			st=conn.createStatement();
			rs=st.executeQuery(TEST_SQL);
		} catch (SQLException ex) {
			System.out.println("测试数据库连接失败！");
			ex.printStackTrace();
		} finally {
			try {
				if (rs!=null) {
					rs.close();
				}
				if (st!=null) {
					st.close();
				}
			} catch (SQLException sqlex) {
				System.out.println("关闭SQL语句资源失败！");
			}
			close(conn);
		}
	}

	public Connection getConnection() {
		Connection conn = null;
		try {
			conn = ds.getConnection();
		} catch (SQLException ex) {
			System.out.println("获取数据库连接失败！");
			ex.printStackTrace();
		}
		return conn;
	}

	public static void close(Connection conn) {
		try {
			if (conn != null && !conn.isClosed()) {
				conn.close();
			}
		} catch (SQLException ex) {
			System.out.println("关闭数据库连接失败！");
			ex.printStackTrace();
		}
	}
}
