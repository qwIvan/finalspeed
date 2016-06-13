// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.rudp.message.AckListMessage;
import net.fs.rudp.message.CloseMessage_Conn;
import net.fs.rudp.message.CloseMessage_Stream;
import net.fs.rudp.message.DataMessage;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;

public class Sender {

	private int sum=0;
	private ConnectionUDP conn;
	private HashMap<Integer, DataMessage> sendTable= new HashMap<>();
	private Object winOb=new Object();
	private InetAddress dstIp;
	private int dstPort;
	private int sequence=0;
	int sendOffset=-1;
	private int unAckMax=-1;
	private int sendSum=0;
	private int sw=0;

	private boolean streamClosed=false;
	
	private static int s=0;
	
	private Object syn_send_table=new Object();

	Sender(ConnectionUDP conn){
		this.conn=conn;
		UDPOutputStream uos = new UDPOutputStream(conn);
		Receiver receiver = conn.receiver;
		this.dstIp=conn.dstIp;
		this.dstPort=conn.dstPort;
	}
	
	void sendData(byte[] data,int offset,int length) throws ConnectException, InterruptedException{
		int packetLength=RUDPConfig.packageSize;
		int sum=(length/packetLength);
		if(length%packetLength!=0){
			sum+=1;
		}
		if(sum==0){
			sum=1;
		}
		int len=packetLength;
		if(length<=len){
			sw++;
			sendNata(data, length);
			sw--;
		}else{
			for(int i=0;i<sum;i++){
				byte[] b=new byte[len];
				System.arraycopy(data, offset, b, 0, len);
				sendNata(b, b.length);
				offset+=packetLength;
				if(offset+len>length){
					len=length-(sum-1)*packetLength;
				}
			}
		}
	}
	
	 private void sendNata(byte[] data, int length) throws ConnectException, InterruptedException{

		 boolean closed = false;
		 if(!closed){
			if(!streamClosed){
				DataMessage me=new DataMessage(sequence,data,0,(short) length,conn.connectId,conn.route.localclientId);
				me.setDstAddress(dstIp);
				me.setDstPort(dstPort);
				synchronized (syn_send_table) {
					sendTable.put(me.getSequence(),me);
				}
				
				synchronized (winOb){
					if(!conn.receiver.checkWin()){
						try {
							winOb.wait();
						} catch (InterruptedException e) {
							throw e;
						}
					}
				}
				
				boolean twice=false;
				if(RUDPConfig.twice_tcp){
					twice=true;
				}
				if(RUDPConfig.double_send_start){
					if(me.getSequence()<=5){
						twice=true;
					}
				}
				sendDataMessage(me,false,twice,true);
				long lastSendTime = System.currentTimeMillis();
				sendOffset++;
				s+=me.getData().length;
				conn.clientControl.resendMange.addTask(conn, sequence);
				sequence++;//必须放最后
			}else{
				throw new ConnectException();
			}
		}else{
			throw new ConnectException();
		}
	
	}
	
	public void closeStream_Local(){
		if(!streamClosed){
			streamClosed=true;
			conn.receiver.closeStream_Local();
			if(!conn.stopnow){
				sendCloseMessage_Stream();
			}
		}
	}
	
	public void closeStream_Remote(){
		if(!streamClosed){
			streamClosed=true;
		}
	}
	
	private void sendDataMessage(DataMessage me, boolean resend, boolean twice, boolean block){
		synchronized (conn.clientControl.getSynlock()) {
			long startTime=System.nanoTime();
			long t1=System.currentTimeMillis();

			int timeId=conn.clientControl.getCurrentTimeId();

			me.create(timeId);

			SendRecord record_current=conn.clientControl.getSendRecord(timeId);
			if(!resend){
				//第一次发，修改当前时间记录
				me.setFirstSendTimeId(timeId);
				record_current.addSended_First(me.getData().length);
				record_current.addSended(me.getData().length);
			}else {
				//重发，修改第一次发送时间记录
				SendRecord record=conn.clientControl.getSendRecord(me.getFirstSendTimeId());
				record.addResended(me.getData().length);
				record_current.addSended(me.getData().length);
			}
			
			try {
				sendSum++;
				sum++;
				unAckMax++;

				long t=System.currentTimeMillis();
				send(me.getDatagramPacket());
				
				if(twice){
					send(me.getDatagramPacket());//发两次
				}
				if(block){
					conn.clientControl.sendSleep(startTime, me.getData().length);
				}
				TrafficEvent event=new TrafficEvent(me.getData().length,TrafficEvent.type_uploadTraffic);
				Route.fireEvent(event);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	void sendAckDelay(int ackSequence){
		conn.route.delayAckManage.addAck(conn, ackSequence);
	}
	
	void sendLastReadDelay(){
		conn.route.delayAckManage.addLastRead(conn);
	}
	
	DataMessage getDataMessage(int sequence){
		return sendTable.get(sequence);
	}

	public void reSend(int sequence){
		if(sendTable.containsKey(sequence)){
			DataMessage dm=sendTable.get(sequence);
			if(dm!=null){
				sendDataMessage(dm,true,false,true);
			}
		}
	}
	
	public void destroy(){
		synchronized (syn_send_table) {
			sendTable.clear();
		}
	}
	
	//删除后不会重发
	void removeSended_Ack(int sequence){
		synchronized (syn_send_table) {
			DataMessage dm=sendTable.remove(sequence);
		}
	}

	void play(){
		synchronized (winOb){
			winOb.notifyAll();
		}
	}

	private void sendCloseMessage_Stream(){
		CloseMessage_Stream cm=new CloseMessage_Stream(conn.connectId,conn.route.localclientId,sequence);
		cm.setDstAddress(dstIp);
		cm.setDstPort(dstPort);
		try {
			send(cm.getDatagramPacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			send(cm.getDatagramPacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void sendCloseMessage_Conn(){
		CloseMessage_Conn cm=new CloseMessage_Conn(conn.connectId,conn.route.localclientId);
		cm.setDstAddress(dstIp);
		cm.setDstPort(dstPort);
		try {
			send(cm.getDatagramPacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
		try {
			send(cm.getDatagramPacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	void sendALMessage(ArrayList ackList){
		int currentTimeId=conn.receiver.getCurrentTimeId();
		AckListMessage alm=new AckListMessage(ackList,conn.receiver.lastRead,conn
				.clientControl.sendRecordTable_remote,currentTimeId,
				conn.connectId,conn.route.localclientId);
		alm.setDstAddress(dstIp);
		alm.setDstPort(dstPort);
		try {
			send(alm.getDatagramPacket());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void send(DatagramPacket dp) throws IOException {
		sendPacket(dp);
	}
	
	private void sendPacket(DatagramPacket dp) throws IOException{
		conn.clientControl.sendPacket(dp);
	}
	
}
