// Copyright (c) 2015 D1SM.net

package net.fs.cap;

import net.fs.rudp.CopiedIterator;

import java.util.HashMap;
import java.util.Iterator;

class TunManager {
	
	private HashMap<String, TCPTun> connTable= new HashMap<>();


	private TCPTun defaultTcpTun;

	private Object syn_scan=new Object();
	
	private CapEnv capEnv;
	
	{
		Thread scanThread = new Thread() {
			public void run() {
				while (true) {
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					scan();
				}
			}
		};
		scanThread.start();
	}
	
	TunManager(CapEnv capEnv){
		this.capEnv=capEnv;
	}
	
	private void scan(){
		Iterator<String> it=getConnTableIterator();
		while(it.hasNext()){
			String key=it.next();
			TCPTun tun=connTable.get(key);
			if(tun!=null){
				if(tun.preDataReady){
					//无数据超时
					long t=System.currentTimeMillis()-tun.lastReceiveDataTime;
					if(t>6000){
						connTable.remove(key);
						if(capEnv.client){
							defaultTcpTun=null;
							System.out.println("tcp隧道超时");
						}
					}
				}else{
					//连接中超时
					if(System.currentTimeMillis()-tun.createTime>5000){
						connTable.remove(key);
					}
				}
			}
		}
	}
	
	void removeTun(TCPTun tun){
		connTable.remove(tun.key);
	}
	
	private Iterator<String> getConnTableIterator(){
		Iterator<String> it;
		synchronized (syn_scan) {
			it=new CopiedIterator(connTable.keySet().iterator());
		}
		return it;
	}

	TCPTun getTcpConnection_Client(String remoteAddress, short remotePort, short localPort){
		return connTable.get(remoteAddress+":"+remotePort+":"+localPort);
	}
	
	void addConnection_Client(TCPTun conn) {
		synchronized (syn_scan) {
			String key=conn.remoteAddress.getHostAddress()+":"+conn.remotePort+":"+conn.localPort;
			//System.out.println("addConnection "+key);
			conn.setKey(key);
			connTable.put(key, conn);
		}
	}
	
	TCPTun getTcpConnection_Server(String remoteAddress, short remotePort){
		return connTable.get(remoteAddress+":"+remotePort);
	}
	
	void addConnection_Server(TCPTun conn) {
		synchronized (syn_scan) {
			String key=conn.remoteAddress.getHostAddress()+":"+conn.remotePort;
			//System.out.println("addConnection "+key);
			conn.setKey(key);
			connTable.put(key, conn);
		}
	}

	TCPTun getDefaultTcpTun() {
		return defaultTcpTun;
	}

	void setDefaultTcpTun(TCPTun defaultTcpTun) {
		this.defaultTcpTun = defaultTcpTun;
	}

}
