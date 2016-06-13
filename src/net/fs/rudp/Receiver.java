// Copyright (c) 2015 D1SM.net

package net.fs.rudp;
import net.fs.rudp.message.AckListMessage;
import net.fs.rudp.message.CloseMessage_Conn;
import net.fs.rudp.message.CloseMessage_Stream;
import net.fs.rudp.message.DataMessage;
import net.fs.utils.MessageCheck;

import java.net.DatagramPacket;
import java.util.ArrayList;
import java.util.HashMap;


public class Receiver {
	private ConnectionUDP conn;
	private HashMap<Integer, DataMessage> receiveTable= new HashMap<>();
	int lastRead=-1;
	private final Object availOb=new Object();

	private int lastRead2=-1;

	private float availWin=RUDPConfig.maxWin;

	private int currentRemoteTimeId;

	private int closeOffset;

	private boolean streamClose=false;

	private boolean reveivedClose=false;

	Receiver(ConnectionUDP conn){
		this.conn=conn;
		new UDPInputStream(conn);
	}

	//接收流数据
	byte[] receive() throws ConnectException {
		DataMessage me;
		if(conn.isConnected()){
			me=receiveTable.get(lastRead+1);
			synchronized (availOb){
				if(me==null){
					//MLog.println("等待中 "+conn.connectId+" "+(lastRead+1));

					try {
						availOb.wait();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					me=receiveTable.get(lastRead+1);
					//MLog.println("等待完成aaa "+conn.connectId+" "+(lastRead+1));
				}
			}

		}else{
			throw new ConnectException();
		}

		if(!streamClose){
			checkCloseOffset_Remote();
			if(me==null){
				throw new ConnectException();
			}
			conn.sender.sendLastReadDelay();

			lastRead++;
			synchronized (availOb){
				receiveTable.remove(me.getSequence());
			}

			//System.out.println("received "+received/1024/1024+"MB");
			return me.getData();
		}else{
			throw new ConnectException();
		}
	}

	public void onReceivePacket(DatagramPacket dp){
		DataMessage me;
		if(dp!=null){
			if(conn.isConnected()){
				int ver=MessageCheck.checkVer(dp);
				int sType=MessageCheck.checkSType(dp);
				if(ver==RUDPConfig.protocal_ver){
					if(sType==net.fs.rudp.message.MessageType.sType_DataMessage){
						me=new DataMessage(dp);
						int timeId=me.getTimeId();
						SendRecord record=conn.clientControl.sendRecordTable_remote.get(timeId);
						if(record==null){
							record=new SendRecord();
							conn.clientControl.sendRecordTable_remote.put(timeId, record);
						}
						record.addSended(me.getData().length);

						if(timeId>currentRemoteTimeId){
							currentRemoteTimeId=timeId;
						}

						int sequence=me.getSequence();

						conn.sender.sendAckDelay(me.getSequence());
						if(sequence>lastRead){
							synchronized (availOb){
								receiveTable.put(sequence, me);
								if(receiveTable.containsKey(lastRead+1)){
									availOb.notify();
								}
							}
						}
					}else if(sType==net.fs.rudp.message.MessageType.sType_AckListMessage){
						AckListMessage alm=new AckListMessage(dp);
						int lastRead3=alm.getLastRead();
						if(lastRead3>lastRead2){
							lastRead2=lastRead3;
						}
						ArrayList ackList=alm.getAckList();

						for (Object anAckList : ackList) {
							int sequence = (Integer) anAckList;
							conn.sender.removeSended_Ack(sequence);
						}
						SendRecord rc1=conn.clientControl.getSendRecord(alm.getR1());
						if(rc1!=null){
							if(alm.getS1()>rc1.getAckedSize()){
								rc1.setAckedSize(alm.getS1());
							}
						}

						SendRecord rc2=conn.clientControl.getSendRecord(alm.getR2());
						if(rc2!=null){
							if(alm.getS2()>rc2.getAckedSize()){
								rc2.setAckedSize(alm.getS2());
							}
						}

						SendRecord rc3=conn.clientControl.getSendRecord(alm.getR3());
						if(rc3!=null){
							if(alm.getS3()>rc3.getAckedSize()){
								rc3.setAckedSize(alm.getS3());
							}
						}

						if(checkWin()){
							conn.sender.play();
						}
					}else if(sType==net.fs.rudp.message.MessageType.sType_CloseMessage_Stream){
						CloseMessage_Stream cm=new CloseMessage_Stream(dp);
						reveivedClose=true;
						int n=cm.getCloseOffset();
						closeStream_Remote(n);
					}else if(sType==net.fs.rudp.message.MessageType.sType_CloseMessage_Conn){
						CloseMessage_Conn cm2=new CloseMessage_Conn(dp);
						conn.close_remote();
					}else{
						////#MLog.println("未处理数据包 "+sType);
					}
				}

			}
		}

	}

	public void destroy(){
		//#MLog.println("destroy destroy destroy");
		synchronized (availOb) {
			receiveTable.clear();
		}
	}

	boolean checkWin(){
		int nw = conn.sender.sendOffset - lastRead2;
		boolean b=false;
		if(nw <availWin){
			b= true;
		}else {
		}
		return b;
	}

	private void closeStream_Remote(int closeOffset){
		this.closeOffset=closeOffset;
		if(!streamClose){
			checkCloseOffset_Remote();
		}
	}

	private void checkCloseOffset_Remote(){
		if(!streamClose){
			if(reveivedClose){
				if(lastRead>=closeOffset-1){
					streamClose=true;
					synchronized (availOb){
						availOb.notifyAll();
					}
					conn.sender.closeStream_Remote();
				}
			}
		}
	}

	void closeStream_Local(){
		if(!streamClose){
			streamClose=true;
			synchronized (availOb){
				availOb.notifyAll();
			}
			conn.sender.closeStream_Local();
		}
	}

	public int getCurrentTimeId() {
		return currentRemoteTimeId;
	}


}
