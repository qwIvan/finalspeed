// Copyright (c) 2015 D1SM.net

package net.fs.rudp;
import java.net.InetAddress;

public class ConnectionUDP {
	public InetAddress dstIp;
	public int dstPort;
	public Sender sender;
	public Receiver receiver;
	public UDPOutputStream uos;
	public UDPInputStream uis;
	Route route;
	private boolean connected=true;

	int connectId;

	public ClientControl clientControl;
	
	private boolean localClosed=false;
	private boolean remoteClosed=false;
	private boolean destroied=false;
	
	public boolean stopnow=false;
	
	public ConnectionUDP(Route ro,InetAddress dstIp,int dstPort,int mode,int connectId,ClientControl clientControl) {
		this.clientControl=clientControl;
		this.route=ro;
		this.dstIp=dstIp;
		this.dstPort=dstPort;
		if(mode==1){
			//MLog.println("                 发起连接RUDP "+dstIp+":"+dstPort+" connectId "+connectId);
		}else if(mode==2){
			
			//MLog.println("                 接受连接RUDP "+dstIp+":"+dstPort+" connectId "+connectId);
		}
		this.connectId=connectId;
		try {
			sender=new Sender(this);
			receiver=new Receiver(this);
			uos=new UDPOutputStream (this);
			uis=new UDPInputStream(this);
			if(mode==2){
				ro.createTunnelProcessor().process(this);
			}
		} catch (Exception e) {
			e.printStackTrace();
			connected=false;
			route.connTable.remove(connectId);
			e.printStackTrace();
			//#MLog.println("                 连接失败RUDP "+connectId);
			synchronized(this){
				notifyAll();
			}
			throw e;
		}
		    //#MLog.println("                 连接成功RUDP "+connectId);
		    synchronized(this){
				notifyAll();
			}
	}

	@Override
	public String toString(){
		return dstIp + ":" + dstPort;
	}
	
	public boolean isConnected(){
		return connected;
	}
	
	public void close_local(){
		if(!localClosed){
			localClosed=true;
			if(!stopnow){
				sender.sendCloseMessage_Conn();
			}
			destroy(false);
		}
	}
	
	public void close_remote() {
		if(!remoteClosed){
			remoteClosed=true;
			destroy(false);
		}
	}
	
	//完全关闭
	public void destroy(boolean force){
		if(!destroied){
			if((localClosed&&remoteClosed)||force){
				destroied=true;
				connected=false;
				uis.closeStream_Local();
				uos.closeStream_Local();
				sender.destroy();
				receiver.destroy();
				route.removeConnection(this);
				clientControl.removeConnection(this);
			}
		}
	}

}
