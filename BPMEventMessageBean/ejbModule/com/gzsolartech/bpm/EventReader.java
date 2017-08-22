package com.gzsolartech.bpm;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.gzsolartech.bpm.utils.BpmGlobalConfigOracleHelper;
import com.gzsolartech.bpm.utils.BpmPushMsgOracleHelper;
import com.gzsolartech.bpm.utils.IBpmGlobalConfigHelper;

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
				String resultMsg=send(msgId, pullMsgUrl);
				System.out.println("msgId="+msgId+" response result"+separator+resultMsg);
				
				//////////////test /////////////////////////
//				resultMsg=send(msgId, "http://192.168.1.69:8083/smartforms/"+PULL_MSG_CTXPATH);
//				System.out.println("response2 result::=============="+resultMsg);
				//////////////////////////
			}
		}).start();
	}

	public String send(String msgId, String addressUrl) {
		String result="";
		try {
			URL url = new URL(addressUrl);
			System.out.println("addressUrl"+separator+addressUrl);
			URLConnection con = url.openConnection();
			con.setDoOutput(true);
            con.setDoInput(true);
			con.setRequestProperty("Pragma:", "no-cache");
			con.setRequestProperty("Cache-Control", "no-cache");
			con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			OutputStreamWriter out = new OutputStreamWriter(
					con.getOutputStream());
			// out.write(new String(msg.getText().getBytes("ISO-8859-1")));
//			out.write(new String(msg.getBytes("utf-8")));
			out.write("msgId="+msgId);
			out.flush();
			out.close();
			BufferedReader br = new BufferedReader(new InputStreamReader(
					con.getInputStream()));
			result=br.readLine();
			br.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		return result;
	}
}
