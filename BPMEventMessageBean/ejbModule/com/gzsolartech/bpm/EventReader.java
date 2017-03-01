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
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.MessageListener;
import javax.jms.TextMessage;

/**
 * Message-Driven Bean implementation class for: EventReader
 */
@MessageDriven(
		activationConfig = { @ActivationConfigProperty(
				propertyName = "destination", propertyValue = "bpd"), @ActivationConfigProperty(
				propertyName = "destinationType", propertyValue = "javax.jms.Queue")
		}, 
		mappedName = "bpd")
public class EventReader implements MessageListener {
	public static final String postUrl = "http://192.168.1.69:8080/smartforms/console/bpm/taskInfo/push.xsp";
	public static final String gfPostUrl = "http://192.168.1.69:8081/gf/console/bpm/taskInfo/push.xsp";
	//public static final String postUrl = "http://10.161.2.170:80/smartforms/console/bpm/taskInfo/push.xsp";
	//public static final String postUrl = "http://10.161.131.11/smartforms/console/bpm/taskInfo/push.xsp";
	//public static final String postUrl = "http://192.168.1.26:8080/smartforms/console/bpm/taskInfo/push.xsp";
	
//	public static final String postUrl="http://192.168.1.147:9081/smartforms/console/bpm/taskInfo/push.xsp";
	
	
	/**
     * Default constructor. 
     */
    public EventReader() {
        //nothing to do
    }
  
	
	/**
     * @see MessageListener#onMessage(Message)
     */
    public void onMessage(Message inMessage) {
    	
    	
    	TextMessage msg = null;

        try {//do things on receiving the event xml
            if (inMessage instanceof TextMessage) {
                msg = (TextMessage) inMessage;
                System.out.println("MESSAGE BEAN: Message received: " +  msg.getText());
                thread(msg.getText(),postUrl);
                thread(msg.getText(),gfPostUrl);
            } else {
                System.out.println("Message of wrong type: " +
                    inMessage.getClass().getName());
            }
        } catch (JMSException e) {
            e.printStackTrace();
            
        } catch (Throwable te) {
            te.printStackTrace();
        }
        
    }

    
    public void thread(final String msg,final String addressUrl){
    	 new Thread(new Runnable() {
    		            @Override
    		              public void run() {     
    		            	send(msg,addressUrl);
    	             }
    	         }) {}.start();
    }
    
    public void send(String msg,String addressUrl){
    	try {
            URL url = new URL(addressUrl);
            URLConnection con = url.openConnection();
            con.setDoOutput(true);
            con.setRequestProperty("Pragma:", "no-cache");
            con.setRequestProperty("Cache-Control", "no-cache");
            con.setRequestProperty("Content-Type", "text/xml");
            OutputStreamWriter out = new OutputStreamWriter(con
                    .getOutputStream());    
           // out.write(new String(msg.getText().getBytes("ISO-8859-1")));
            out.write(new String(msg.getBytes("utf-8")));
            out.flush();
            out.close();
            try {
				BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
				br.close();
			} catch (Exception e) {
				e.printStackTrace();
				//无需响应，但去除有错
			}
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
