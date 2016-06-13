// Copyright (c) 2015 D1SM.net

package net.fs.rudp.message;


import java.net.DatagramPacket;

import net.fs.utils.ByteIntConvert;
import net.fs.utils.ByteShortConvert;






public class CloseMessage_Stream extends Message{
	
	private short sType=net.fs.rudp.message.MessageType.sType_CloseMessage_Stream;

	private int closeOffset;
	
	public CloseMessage_Stream(int connectId,int clientId,int closeOffset){
		byte[] dpData=new byte[16];
		this.clientId=clientId;
		this.connectId=connectId;
		ByteShortConvert.toByteArray(ver, dpData, 0);  //add: ver
		ByteShortConvert.toByteArray(sType, dpData, 2);  //add: service type
		ByteIntConvert.toByteArray(connectId, dpData, 4); //add: sequence
		ByteIntConvert.toByteArray(clientId, dpData, 8); //add: sequence
		ByteIntConvert.toByteArray(closeOffset, dpData, 12); //add: sequence
		dp=new DatagramPacket(dpData,dpData.length);
		////#System.out.println("vCloseMessageaaa"+clientId+"v");
	}
	
	public CloseMessage_Stream(DatagramPacket dp){
		this.dp=dp;
		byte[] dpData = dp.getData();
		ver=ByteShortConvert.toShort(dpData, 0);
		sType=ByteShortConvert.toShort(dpData, 2);
		
		connectId=ByteIntConvert.toInt(dpData, 4);
		clientId=ByteIntConvert.toInt(dpData, 8);
		closeOffset=ByteIntConvert.toInt(dpData, 12);
		////#System.out.println("vCloseMessagebbb"+clientId+"v");
	}
	
	public int getCloseOffset(){
		return closeOffset;
	}
	
}
