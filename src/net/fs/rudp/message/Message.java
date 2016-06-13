// Copyright (c) 2015 D1SM.net

package net.fs.rudp.message;


import java.net.DatagramPacket;
import java.net.InetAddress;

import net.fs.rudp.RUDPConfig;

public abstract class Message {
	
	short ver=RUDPConfig.protocal_ver;
	short sType=0;
	DatagramPacket dp;
	int connectId;
	int clientId;

	public DatagramPacket getDatagramPacket(){
		return dp;
	}
	public void setDstAddress(InetAddress dstIp){
		dp.setAddress(dstIp);
	}
	public void setDstPort(int dstPort){
		dp.setPort(dstPort);
	}

}
