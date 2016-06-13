// Copyright (c) 2015 D1SM.net

package net.fs.rudp.message;

import net.fs.utils.ByteIntConvert;
import net.fs.utils.ByteShortConvert;

import java.net.DatagramPacket;



public class PingMessage extends Message{
	
	private short sType=net.fs.rudp.message.MessageType.sType_PingMessage;
	
	private byte[] dpData=new byte[20];
	
	private int pingId;
	private int downloadSpeed;

	public PingMessage(int connectId,int clientId,int pingId,int downloadSpeed,int uploadSpeed){
		ByteShortConvert.toByteArray(ver, dpData, 0);  //add: ver
		ByteShortConvert.toByteArray(sType, dpData, 2);  //add: service type
		ByteIntConvert.toByteArray(connectId, dpData, 4); //add: sequence
		ByteIntConvert.toByteArray(clientId, dpData, 8); //add: sequence
		ByteIntConvert.toByteArray(pingId, dpData, 12); //add: sequence
		ByteShortConvert.toByteArray((short) (downloadSpeed/1024), dpData, 16);
		ByteShortConvert.toByteArray((short) (uploadSpeed/1024), dpData, 18);
		dp=new DatagramPacket(dpData,dpData.length);
	}
	
	public PingMessage(DatagramPacket dp){
		this.dp=dp;
		dpData=dp.getData();
		ver=ByteShortConvert.toShort(dpData, 0);
		sType=ByteShortConvert.toShort(dpData, 2);
		connectId=ByteIntConvert.toInt(dpData, 4);
		clientId=ByteIntConvert.toInt(dpData, 8);
		pingId=ByteIntConvert.toInt(dpData, 12);
		downloadSpeed=ByteShortConvert.toShort(dpData, 16);
	}

	public int getPingId() {
		return pingId;
	}

	public int getDownloadSpeed() {
		return downloadSpeed;
	}

}
