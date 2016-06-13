// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

import net.fs.cap.CapEnv;
import net.fs.cap.VDatagramSocket;
import net.fs.rudp.message.MessageType;
import net.fs.utils.ByteIntConvert;
import net.fs.utils.MessageCheck;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;


public class Route {

	private DatagramSocket ds;
	public HashMap<Integer, ConnectionUDP> connTable;

	public static ThreadPoolExecutor es;
	
	public AckListManage delayAckManage;

	private Object syn_ds2Table=new Object();

	private Random ran=new Random();

	public int localclientId=Math.abs(ran.nextInt());

	private LinkedBlockingQueue<DatagramPacket> packetBuffer= new LinkedBlockingQueue<>();
	
	public static int mode_server=2;
	
	public static int mode_client=1;

	public int mode=mode_client;//1客户端,2服务端
	
	private String pocessName="";

	private HashSet<Integer> setedTable= new HashSet<>();

	private HashSet<Integer> closedTable= new HashSet<>();

	public static int localDownloadSpeed,localUploadSpeed;

	ClientManager clientManager;

	public CapEnv capEnv=null;
	
	public ClientControl lastClientControl;
	
	private boolean useTcpTun=true;

	private static List<Trafficlistener> listenerList= new Vector<>();
	
	{
		
		delayAckManage = new AckListManage();
	}
	
	static{
		SynchronousQueue queue = new SynchronousQueue();
		es= new ThreadPoolExecutor(100, Integer.MAX_VALUE, 10*1000, TimeUnit.MILLISECONDS, queue);
	}
	
	public Route(String pocessName,short routePort,int mode2,boolean tcp,boolean tcpEnvSuccess) throws Exception{
		
		this.mode=mode2;
		useTcpTun=tcp;
		this.pocessName=pocessName;
		if(useTcpTun){
			if(mode==2){
				//服务端
				VDatagramSocket d=new VDatagramSocket(routePort);
				d.setClient(false);
				try {
					capEnv=new CapEnv(false,tcpEnvSuccess);
					capEnv.setListenPort(routePort);
					capEnv.init();
				} catch (Exception e) {
					//e.printStackTrace();
					throw e;
				} 
				d.setCapEnv(capEnv);
				
				ds=d;
			}else {
				//客户端
				VDatagramSocket d=new VDatagramSocket();
				d.setClient(true);
				try {
					capEnv=new CapEnv(true,tcpEnvSuccess);
					capEnv.init();
				} catch (Exception e) {
					//e.printStackTrace();
					throw e;
				} 
				d.setCapEnv(capEnv);
				
				ds=d;
			}
		}else {
			if(mode==2){
				System.out.println("Listen udp port: "+CapEnv.toUnsigned(routePort));
				ds=new DatagramSocket(CapEnv.toUnsigned(routePort));
			}else {
				ds=new DatagramSocket();
			}
		}
		
		connTable= new HashMap<>();
		clientManager=new ClientManager(this);
		Thread reveiveThread = new Thread() {
			@Override
			public void run() {
				while (true) {
					byte[] b = new byte[1500];
					DatagramPacket dp = new DatagramPacket(b, b.length);
					try {
						ds.receive(dp);
						//System.out.println("接收 "+dp.getAddress());
						packetBuffer.add(dp);
					} catch (IOException e) {
						e.printStackTrace();
						try {
							Thread.sleep(1);
						} catch (InterruptedException e1) {
							e1.printStackTrace();
						}
						continue;
					}
				}
			}
		};
		reveiveThread.start();

		Thread mainThread = new Thread() {
			public void run() {
				while (true) {
					DatagramPacket dp = null;
					try {
						dp = packetBuffer.take();
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
					if (dp == null) {
						continue;
					}
					long t1 = System.currentTimeMillis();
					byte[] dpData = dp.getData();

					int sType = 0;
					if (dp.getData().length < 4) {
						return;
					}
					sType = MessageCheck.checkSType(dp);
					//System.out.println("route receive MessageType111#"+sType+" "+dp.getAddress()+":"+dp.getPort());
					if (dp != null) {

						final int connectId = ByteIntConvert.toInt(dpData, 4);
						int remote_clientId = ByteIntConvert.toInt(dpData, 8);

						if (closedTable.contains(connectId) && connectId != 0) {
							//#System.out.println("忽略已关闭连接包 "+connectId);
							continue;
						}

						if (sType == MessageType.sType_PingMessage
								|| sType == MessageType.sType_PingMessage2) {
							ClientControl clientControl = null;
							if (mode == 2) {
								//发起
								clientControl = clientManager.getClientControl(remote_clientId, dp.getAddress(), dp.getPort());
							} else if (mode == 1) {
								//接收
								String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
								int sim_clientId = Math.abs(key.hashCode());
								clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
							}
							clientControl.onReceivePacket(dp);
						} else {
							//发起
							if (mode == 1) {
								if (!setedTable.contains(remote_clientId)) {
									String key = dp.getAddress().getHostAddress() + ":" + dp.getPort();
									int sim_clientId = Math.abs(key.hashCode());
									ClientControl clientControl = clientManager.getClientControl(sim_clientId, dp.getAddress(), dp.getPort());
									if (clientControl.getClientId_real() == -1) {
										clientControl.setClientId_real(remote_clientId);
										//#System.out.println("首次设置clientId "+remote_clientId);
									} else {
										if (clientControl.getClientId_real() != remote_clientId) {
											//#System.out.println("服务端重启更新clientId "+sType+" "+clientControl.getClientId_real()+" new: "+remote_clientId);
											clientControl.updateClientId(remote_clientId);
										}
									}
									//#System.out.println("cccccc "+sType+" "+remote_clientId);
									setedTable.add(remote_clientId);
								}
							}


							//udp connection
							if (mode == 2) {
								//接收
								try {
									getConnection2(dp.getAddress(), dp.getPort(), connectId, remote_clientId);
								} catch (Exception e) {
									e.printStackTrace();
								}
							}

							final ConnectionUDP ds3 = connTable.get(connectId);
							if (ds3 != null) {
								ds3.receiver.onReceivePacket(dp);
								if (sType == MessageType.sType_DataMessage) {
									TrafficEvent event = new TrafficEvent(dp.getLength(), TrafficEvent.type_downloadTraffic);
									fireEvent(event);
								}
							}

						}
					}
				}
			}
		};
		mainThread.start();
		
	}

	public static void addTrafficlistener(Trafficlistener listener){
		listenerList.add(listener);
	}

	static void fireEvent(TrafficEvent event){
		for(Trafficlistener listener:listenerList){
			int type=event.getType();
			if(type==TrafficEvent.type_downloadTraffic){
				listener.trafficDownload(event);
			}else if(type==TrafficEvent.type_uploadTraffic){
				listener.trafficUpload(event);
			}
		}
	}

	public void sendPacket(DatagramPacket dp) throws IOException{
		ds.send(dp);
	}

	public ConnectionProcessor createTunnelProcessor(){
		ConnectionProcessor o=null;
		try {
			Class onwClass = Class.forName(pocessName);
			o = (ConnectionProcessor) onwClass.newInstance();
		} catch (ClassNotFoundException | IllegalAccessException | InstantiationException e) {
			e.printStackTrace();
		}
		return o;
	}

	void removeConnection(ConnectionUDP conn){
		synchronized (syn_ds2Table){
			closedTable.add(conn.connectId);
			connTable.remove(conn.connectId);
		}
	}

	//接收连接
	private void getConnection2(InetAddress dstIp, int dstPort, int connectId, int clientId) throws Exception{
		ConnectionUDP conn=connTable.get(connectId);
		if(conn==null){
			ClientControl clientControl=clientManager.getClientControl(clientId,dstIp,dstPort);
			conn=new ConnectionUDP(this,dstIp,dstPort,2,connectId,clientControl);
			synchronized (syn_ds2Table){
				connTable.put(connectId, conn);
			}
			clientControl.addConnection(conn);
		}
	}

	//发起连接
	public ConnectionUDP getConnection(String address, int dstPort) throws Exception{
		InetAddress dstIp=InetAddress.getByName(address);
		int connectId=Math.abs(ran.nextInt());
		String key=dstIp.getHostAddress()+":"+dstPort;
		int remote_clientId=Math.abs(key.hashCode());
		ClientControl clientControl=clientManager.getClientControl(remote_clientId,dstIp,dstPort);
		ConnectionUDP conn=new ConnectionUDP(this,dstIp,dstPort,1,connectId,clientControl);
		synchronized (syn_ds2Table){
			connTable.put(connectId, conn);
		}
		clientControl.addConnection(conn);
		lastClientControl=clientControl;
		return conn;
	}

	public boolean isUseTcpTun() {
		return useTcpTun;
	}

}


