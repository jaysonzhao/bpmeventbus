package com.gzsolartech.bpm;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import javax.ejb.ActivationConfigProperty;
import javax.ejb.MessageDriven;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

import com.gzsolartech.bpm.utils.BpmPushMsgOracleHelper;

/**
 * Message-Driven Bean implementation class for: EventReader
 */
@MessageDriven(activationConfig = {
		@ActivationConfigProperty(propertyName = "destination", propertyValue = "bpd"),
		@ActivationConfigProperty(propertyName = "destinationType", propertyValue = "javax.jms.Queue") }, mappedName = "bpd")
public class EventReader implements MessageListener {
	private BpmPushMsgOracleHelper pushMsgHelper=new BpmPushMsgOracleHelper();
	public static final String postUrl = "http://192.168.1.69:8083/smartforms/console/bpm/taskInfo/pullOrigMsg.xsp";
//	public static final String gfPostUrl = "http://192.168.1.69:8081/gf/console/bpm/taskInfo/push.xsp";

	// public static final String postUrl =
	// "http://10.161.2.170:80/smartforms/console/bpm/taskInfo/push.xsp";
	// public static final String postUrl =
	// "http://10.161.131.11/smartforms/console/bpm/taskInfo/push.xsp";
	// public static final String postUrl =
	// "http://192.168.1.26:8080/smartforms/console/bpm/taskInfo/push.xsp";

	// public static final String
	// postUrl="http://192.168.1.147:9081/smartforms/console/bpm/taskInfo/push.xsp";

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
				System.out.println("=========msgId::: "+msgId);
				if (msgId!=null && !msgId.trim().isEmpty()) {
					//再使用多线程的方式通知消息解析器到数据库中获取消息，然后进行解析
					thread(msgId, postUrl);
				}
//				thread(msg.getText(), postUrl);
//				thread(msg.getText(), gfPostUrl);
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

//	public void thread(final String msg, final String addressUrl) {
//		new Thread(new Runnable() {
//			@Override
//			public void run() {
//				send(msg, addressUrl);
//			}
//		}) {
//		}.start();
//	}
	
	public void thread(final String msgId, final String pullMsgUrl) {
		new Thread(new Runnable() {
			@Override
			public void run() {
				send(msgId, pullMsgUrl);
			}
		}).start();
	}

	public void send(String msgId, String addressUrl) {
		try {
			URL url = new URL(addressUrl);
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
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(
						con.getInputStream()));
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
				// 无需响应，但去除有错
			}
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
