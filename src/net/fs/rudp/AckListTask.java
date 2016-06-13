// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.util.ArrayList;
import java.util.HashSet;



class AckListTask {
	private ConnectionUDP conn;
	private ArrayList<Integer> ackList;
	@SuppressWarnings("unchecked")
	private
	HashSet set;
	AckListTask(ConnectionUDP conn){
		this.conn=conn;
		ackList=new ArrayList();
		set=new HashSet();
	}
	
	synchronized void addAck(int sequence){
		////#System.out.println("sendACK "+sequence);
		if(!set.contains(sequence)){
			ackList.add(sequence);
			set.add(sequence);
		}
	}
	
	synchronized void run(){
		int offset=0;
		int packetLength=RUDPConfig.ackListSum;
		int length=ackList.size();
		////#System.out.println("ffffffffaaaaaaaaa "+length);
		int sum=(length/packetLength);
		if(length%packetLength!=0){
			sum+=1;
		}
		if(sum==0){
			sum=1;
		}
		int len=packetLength;
		if(length<=len){
			conn.sender.sendALMessage(ackList);
			conn.sender.sendALMessage(ackList);
		}else{
			for(int i=0;i<sum;i++){
				ArrayList<Integer> nl=copy(offset,len,ackList);
				conn.sender.sendALMessage(nl);
				conn.sender.sendALMessage(nl);
//				conn.sender.sendALMessage(nl);
//				conn.sender.sendALMessage(nl);
//				conn.sender.sendALMessage(nl);
				offset+=packetLength;
				////#System.out.println("fffffffffa "+nl.size());
				if(offset+len>length){
					len=length-(sum-1)*packetLength;
				}
			}
		}
	}
	
	private ArrayList<Integer> copy(int offset, int length, ArrayList<Integer> ackList){
		ArrayList<Integer> nl= new ArrayList<>();
		for(int i=0;i<length;i++){
			nl.add(ackList.get(offset+i));
		}
		return nl;
	}
}
