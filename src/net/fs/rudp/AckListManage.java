// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import java.util.HashMap;

public class AckListManage implements Runnable{
	private HashMap<Integer, AckListTask> taskTable;
	public AckListManage(){
		taskTable= new HashMap<>();
		Thread mainThread = new Thread(this);
		mainThread.start();
	}
	
	synchronized void addAck(ConnectionUDP conn,int sequence){
		if(!taskTable.containsKey(conn.connectId)){
			AckListTask at=new AckListTask(conn);
			taskTable.put(conn.connectId, at);
		}
		AckListTask at=taskTable.get(conn.connectId);
		at.addAck(sequence);
	}
	
	synchronized void addLastRead(ConnectionUDP conn){
		if(!taskTable.containsKey(conn.connectId)){
			AckListTask at=new AckListTask(conn);
			taskTable.put(conn.connectId, at);
		}
	}
	
	public void run(){
		while(true){
			synchronized (this){
				for (Integer id : taskTable.keySet()) {
					AckListTask at = taskTable.get(id);
					at.run();
				}
				taskTable.clear();
				taskTable=null;
				taskTable= new HashMap<>();
			}
			
			try {
				Thread.sleep(RUDPConfig.ackListDelay);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
}
