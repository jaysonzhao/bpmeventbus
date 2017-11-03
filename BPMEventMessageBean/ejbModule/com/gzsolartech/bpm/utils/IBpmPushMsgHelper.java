package com.gzsolartech.bpm.utils;

import java.sql.Connection;

public interface IBpmPushMsgHelper {
	public String createMsg(Connection conn, String msgBody);
}
