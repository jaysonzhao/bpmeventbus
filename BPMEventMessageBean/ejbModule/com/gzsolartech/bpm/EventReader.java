package com.gzsolartech.bpm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Map;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.gzsolartech.bpm.utils.BpmGlobalConfigOracleHelper;
import com.gzsolartech.bpm.utils.BpmPushMsgOracleHelper;
import com.gzsolartech.bpm.utils.IBpmGlobalConfigHelper;
import com.gzsolartech.bpm.utils.OracleJdbcUtils;

/**
 * Message-Driven Bean implementation class for: EventReader
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "bpd"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") }, mappedName = "bpd")
public class EventReader implements MessageListener {
	private BpmPushMsgOracleHelper pushMsgHelper=new BpmPushMsgOracleHelper();
	private IBpmGlobalConfigHelper bpmgcfgHelper=new BpmGlobalConfigOracleHelper();
	private final String PULL_MSG_CTXPATH="console/bpm/taskInfo/pullOrigMsg.xsp";
	private final String separator=" ======== ";
//	public static final String postUrl = "http://192.168.1.110:7788/smartforms/console/bpm/taskInfo/pullOrigMsg.xsp";
//	public static final String postUrl = "http://10.161.2.170:9081/smartforms/console/bpm/taskInfo/pullOrigMsg.xsp";
//	public static final String postUrl = "http://10.161.131.15/console/bpm/taskInfo/pullOrigMsg.xsp";
//	public static final String postUrl = "http://192.168.1.69:8083/smartforms/console/bpm/taskInfo/pullOrigMsg.xsp";
//	public static final String gfPostUrl = "http://192.168.1.69:8081/gf/console/bpm/taskInfo/push.xsp";

	/**
	 * Default constructor.
	 */
	public EventReader() {}

	/**
	 * @see MessageListener#onMessage(Message)
	 * 关于TransactionAttributeType注解的更多信息，请查看：
	 * https://www.ibm.com/developerworks/cn/java/j-lo-springejbtrans/index.html
	 */
	@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
	public void onMessage(Message inMessage) {
		TextMessage msg = null;
		try {
			// do things on receiving the event xml
			if (inMessage instanceof TextMessage) {
				msg = (TextMessage) inMessage;
				System.out.println("MESSAGE BEAN: Message received: "+msg.getText());
				//先使用单线程的方式将推送过来的消息存放的数据库
				String msgId=pushMsgHelper.createMsg(msg.getText());
				System.out.println("msgId"+separator+msgId);
				
				//------- 存储到另外一个数据库，临时测试使用 --------------------
				/*String insertMsgSql="insert into BPM_ORIGINAL_PUSH_MSG "
						+ " (MSG_ID, MSG_BODY, CREATE_TIME, UPDATE_TIME) "
						+ " values (?,?,?,?)";
				Connection conn=null;
				PreparedStatement ps=null;
				try {
					Class.forName("oracle.jdbc.driver.OracleDriver").newInstance();
					conn=DriverManager.getConnection("jdbc:oracle:thin:@192.168.1.69:1521:smartformsdb", "aacsmart", "aacsmart");
					ps=conn.prepareStatement(insertMsgSql);
					Timestamp tsnow=new Timestamp(new Date().getTime());
					ps.setString(1, msgId);
					ps.setString(2, msg.getText());
					ps.setTimestamp(3, tsnow);
					ps.setTimestamp(4, tsnow);
					int result=ps.executeUpdate();
					System.out.println("update result===="+result);
				} catch (Exception ex) {
					ex.printStackTrace();
				} finally {
					if (ps!=null) {
						ps.close();
					}
					if (conn!=null) {
						conn.close();
					}
				}*/
				//-------------------------------------
				
				//获取BPM服务器配置信息
				Map<String, Object> dataMap=bpmgcfgHelper.getFirstActConfig();
				String host=(String)dataMap.get("BPMFORMS_HOST");
				host=(host==null) ? "" : host;
				host=host.endsWith("/") ? host.trim() : host.trim()+"/";
				String webctx=(String)dataMap.get("BPMFORMS_WEB_CONTEXT");
				webctx=(webctx==null) ? "" : webctx;
				webctx=webctx.startsWith("/") ? webctx.trim().substring(1) : webctx.trim();
				String postUrl=host+webctx;
				postUrl=(postUrl.endsWith("/")) ? postUrl : postUrl+"/";
				postUrl+=PULL_MSG_CTXPATH;
				if (msgId!=null && !msgId.trim().isEmpty()) {
					//再使用多线程的方式通知消息解析器到数据库中获取消息，然后进行解析
					thread(msgId, postUrl);
				}
			} else {
				System.out.println("Message of wrong type: "
						+ inMessage.getClass().getName());
			}
		} catch (JMSException e) {
			System.out.println("JMS异常！");
			e.printStackTrace();

		} catch (Throwable te) {
			System.out.println("程序异常！");
			te.printStackTrace();
		}
	}
	
	public void thread(final String msgId, final String pullMsgUrl) {
//		Random random=new Random();
//		int max=10;
//	    int min=1;
//	    int rnum = random.nextInt(max)%(max-min+1) + min;
//		try {
//			//休眠1-10秒后再推送消息，防止高并发推送请求的发生
//			Thread.sleep(rnum*1000);
//		} catch (InterruptedException e) {
//			System.out.println("在推送BPM消息前进入休眠状态发生异常！");
//			e.printStackTrace();
//		}
		new Thread(new Runnable() {
			@Override
			public void run() {
				JSONObject jsoResultMsg=send(msgId, pullMsgUrl);
				//将返回结果存储到数据库中
				OracleJdbcUtils orclUtils=new OracleJdbcUtils();
				Connection conn=orclUtils.getConnection();
				StringBuffer sbuf=new StringBuffer();
				sbuf.append("insert into BPM_ORIGINAL_PUSH_MSG_LOG (MSG_ID, RESULT_TYPE, RESULT_MSG, "
						+ " CREATE_TIME, ERROR_MSG) ");
				sbuf.append(" values (?,?,?,?,?)");
				try {
					//JSONObject jsoResult=JSONObject.parseObject(resultMsg);
					PreparedStatement pst=conn.prepareStatement(sbuf.toString());
					pst.setString(1, msgId);
					String result=jsoResultMsg.getString("result");
					String returnType="";
					if ("success".equals(result)) {
						returnType=jsoResultMsg.getString("returnType");
					}
					//resultType是请求消息拉取的URL时返回的结果；记录在RESULT_TYPE字段中
					pst.setString(2, returnType);
					//result是send方法处理结果，一般为success，若抛出异常，就返回failed；
					//记录在RESULT_MSG字段中
					pst.setString(3, result);
					pst.setTimestamp(4, new Timestamp(System.currentTimeMillis()));
					String errmsg=jsoResultMsg.getString("errmsg");
					if (errmsg!=null && errmsg.length()>=4000) {
						errmsg=errmsg.substring(0, 3995);
					}
					pst.setString(5, (errmsg==null) ? "" : errmsg);
					pst.executeUpdate();
					pst.close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					try {
						conn.close();
					} catch (SQLException e) {
						e.printStackTrace();
					}
				}
				//////////////test /////////////////////////
//				resultMsg=send(msgId, "http://192.168.1.69:8083/smartforms/"+PULL_MSG_CTXPATH);
//				System.out.println("response2 result::=============="+resultMsg);
				//////////////////////////
			}
		}).start();
	}

	public JSONObject send(String msgId, String addressUrl) {
		OutputStreamWriter out=null;
		BufferedReader bufrd=null;
		int timeout=60000;
		JSONObject jsoResult=new JSONObject();
		try {
			URL url = new URL(addressUrl);
			System.out.println("addressUrl"+separator+addressUrl+"?msgId="+msgId);
			HttpURLConnection con = (HttpURLConnection)url.openConnection();
			con.setDoOutput(true);
            con.setDoInput(true);
            //设置连接超时为60秒
            con.setConnectTimeout(timeout);
            con.setReadTimeout(timeout);
            con.setRequestMethod("POST");
			con.setRequestProperty("Pragma", "no-cache");
			con.setRequestProperty("Cache-Control", "no-cache");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			out = new OutputStreamWriter(
					con.getOutputStream());
			// out.write(new String(msg.getText().getBytes("ISO-8859-1")));
//			out.write(new String(msg.getBytes("utf-8")));
			out.write("msgId="+msgId);
			out.flush();
			bufrd = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			String result="";
			String line=bufrd.readLine();
			while (line!=null) {
				result+=line;
				line=bufrd.readLine();
			}
			System.out.println("msgId="+msgId+", response result"+separator+result);
			jsoResult.put("result", "success");
			//解析返回结果
			JSONObject jsoTemp=JSONObject.parseObject(result);
			String retResult=jsoTemp.getString("result");
			JSONArray jayTemp=jsoTemp.getJSONArray("msgs");
			String errMsgs="";
			if (jayTemp!=null) {
				for (int i=0; i<jayTemp.size(); i++) {
					errMsgs+=jayTemp.getString(i)+",,,";
				}
			}
			jsoResult.put("returnType", retResult);
			jsoResult.put("errmsg", errMsgs);
		} catch (Exception ex) {
			ex.printStackTrace();
			jsoResult.put("result", "failed");
			jsoResult.put("errmsg", ex.toString());
		} finally {
			if (out!=null) {
				try {
					out.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if (bufrd!=null) {
				try {
					bufrd.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return jsoResult;
	}
}
