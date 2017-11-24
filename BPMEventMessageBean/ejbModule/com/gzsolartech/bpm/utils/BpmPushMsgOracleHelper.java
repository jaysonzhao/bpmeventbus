package com.gzsolartech.bpm.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.UUID;

public class BpmPushMsgOracleHelper implements IBpmPushMsgHelper {
	private final String insertMsgSql="insert into BPM_ORIGINAL_PUSH_MSG "
			+ " (MSG_ID, MSG_BODY, CREATE_TIME, UPDATE_TIME) "
			+ " values (?,?,?,?)";
	
	/**
	 * 将BPM推送消息体存储到数据库中
	 * @param conn 数据库连接
	 * @param msgBody 消息体
	 * @return 返回自定义消息ID
	 */
	@Override
	public String createMsg(Connection conn, String msgBody) {
		String id="pushmsg:"+UUID.randomUUID().toString();
		Timestamp tsnow=new Timestamp(new Date().getTime());
		PreparedStatement ps=null;
		try {
			//将事务级别设置为提交读取
			//读取数据的事务允许其他事务继续访问该行数据，但是未提交写事务将会禁止其他事务访问该行。
			//最严格的事务级别，虽然性能有所下降，但能保证数据一致性。
			conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
			ps=conn.prepareStatement(insertMsgSql);
			ps.setString(1, id);
			ps.setString(2, msgBody);
			ps.setTimestamp(3, tsnow);
			ps.setTimestamp(4, tsnow);
			ps.executeUpdate();
		} catch (Exception ex) {
			System.out.println("创建BPM原始推送消息失败！");
			ex.printStackTrace();
			id="";
		} finally {
			try {
				if (ps!=null) {
					ps.close();
				}
			} catch (Exception ex) {
				System.out.println("释放数据库资源时发生异常！");
				ex.printStackTrace();
			}
		}
		return id;
	}

}
