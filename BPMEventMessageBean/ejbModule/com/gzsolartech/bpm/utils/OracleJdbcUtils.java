package com.gzsolartech.bpm.utils;

import java.sql.Connection;
import java.sql.SQLException;

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
	private final String JNDI_NAME="jndi/smartforms";
	private DataSource ds = null;
	
	public OracleJdbcUtils() {
		try {
			Context initContext = new InitialContext();
			ds = (DataSource) initContext.lookup(JNDI_NAME);// JNDI
		} catch (Exception ex) {
			System.out.println("初始化JNDI连接失败！");
			ex.printStackTrace();
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
