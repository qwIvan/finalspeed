package net.fs.client;

import net.fs.rudp.ClientProcessorInterface;
import net.fs.rudp.Route;
import net.fs.rudp.TrafficEvent;
import net.fs.rudp.Trafficlistener;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashSet;

class MapClient implements Trafficlistener{

	Route route_udp,route_tcp;

	String serverAddress="";

	int serverPort=130;

	private HashSet<ClientProcessorInterface> processTable= new HashSet<>();
	
	private final Object syn_process=new Object();

	private String systemName=System.getProperty("os.name").toLowerCase();
	
	private boolean useTcp=true;

	MapClient(FSClient ui, boolean tcpEnvSuccess) throws Exception {
		try {
			int monPort = 25874;
			final ServerSocket socket=new ServerSocket(monPort);
			new Thread(){
				public void run(){
					try {
						socket.accept();
					} catch (IOException e) {
						e.printStackTrace();
						System.exit(0);
					}
				}
			}.start();
		} catch (Exception e) {
			//e.printStackTrace();
			System.exit(0);
		}
		short routePort = 45;
		route_tcp = new Route(null, routePort,Route.mode_client,true,tcpEnvSuccess);
		route_udp = new Route(null, routePort,Route.mode_client,false,tcpEnvSuccess);

		new PortMapManager(this);


		Route.addTrafficlistener(this);
		
	}

	void setMapServer(String serverAddress, int serverPort, boolean tcp){
		if(this.serverAddress==null
				||!this.serverAddress.equals(serverAddress)
				||this.serverPort!=serverPort){
			
			if(route_tcp.lastClientControl!=null){
				route_tcp.lastClientControl.close();
			} 
			
			if(route_udp.lastClientControl!=null){
				route_udp.lastClientControl.close();
			} 

			cleanRule();
			if(serverAddress!=null&&!serverAddress.equals("")){
				setFireWallRule(serverAddress,serverPort);
			}
			
		}
		this.serverAddress=serverAddress;
		this.serverPort=serverPort;
		useTcp=tcp;
		resetConnection();
	}
	

	private void setFireWallRule(String serverAddress, int serverPort){
		String ip;
		try {
			ip = InetAddress.getByName(serverAddress).getHostAddress();
			String cmd2="iptables -t filter -A OUTPUT -d "+ip+" -p tcp --dport "+serverPort+" -j DROP -m comment --comment tcptun_fs ";
			runCommand(cmd2);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
	}

	private void cleanRule(){
		if(systemName.contains("mac os")){
			cleanTcpTunRule_osx();
		}else if(systemName.contains("linux")){
			cleanTcpTunRule_linux();
		}else {
			try {
				if(systemName.contains("xp")||systemName.contains("2003")){
					String cmd_delete="ipseccmd -p \"tcptun_fs\" -w reg -y";
					final Process p1 = Runtime.getRuntime().exec(cmd_delete,null);
					p1.waitFor();
				}else {
					String cmd_delete="netsh advfirewall firewall delete rule name=tcptun_fs ";
					final Process p1 = Runtime.getRuntime().exec(cmd_delete,null);
					p1.waitFor();
				}
			
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	
	private void cleanTcpTunRule_osx(){
		String cmd2="sudo ipfw delete 5050";
		runCommand(cmd2);
	}
	
	
	private void cleanTcpTunRule_linux(){
		while(true){
			int row=getRow_linux();
			if(row>0){
				//System.out.println("删除行 "+row);
				String cmd="iptables -D OUTPUT "+row;
				runCommand(cmd);
			}else {
				break;
			}
		}
	}

	private int getRow_linux(){
		int row_delect=-1;
		String cme_list_rule="iptables -L -n --line-number";
		//String [] cmd={"netsh","advfirewall set allprofiles state on"};
		Thread errorReadThread;
		try {
			final Process p = Runtime.getRuntime().exec(cme_list_rule,null);

			errorReadThread=new Thread(){
				public void run(){
					InputStream is=p.getErrorStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							if (line == null){ 
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
							//error();
							break;
						}
					}
				}
			};
			errorReadThread.start();



			InputStream is=p.getInputStream();
			BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
			while (true){
				String line; 
				try {
					line = localBufferedReader.readLine();
				//	System.out.println("standaaa "+line);
					if (line == null){ 
						break;
					}else{ 
						if(line.contains("tcptun_fs")){
							int index=line.indexOf("   ");
							if(index>0){
								String n=line.substring(0, index);
								try {
									if(row_delect<0){
										//System.out.println("standaaabbb "+line);
										row_delect=Integer.parseInt(n);
									}
								} catch (Exception ignored) {
								}
							}
						}
					}
				} catch (IOException e) {
					e.printStackTrace();
					break;
				}
			}


			errorReadThread.join();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			//error();
		}
		return row_delect;
	}
	
	private void resetConnection(){
		synchronized (syn_process) {
			
		}
	}
	
	void onProcessClose(ClientProcessorInterface process){
		synchronized (syn_process) {
			processTable.remove(process);
		}
	}

	public void trafficDownload(TrafficEvent event) {
		////#System.out.println("下载 "+event.getTraffic());
		System.currentTimeMillis();
	}

	public void trafficUpload(TrafficEvent event) {
		////#System.out.println("上传 "+event.getTraffic());
		System.currentTimeMillis();
	}

	private static void runCommand(String command){
		Thread standReadThread=null;
		Thread errorReadThread=null;
		try {
			final Process p = Runtime.getRuntime().exec(command,null);
			standReadThread=new Thread(){
				public void run(){
					InputStream is=p.getInputStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							//System.out.println("stand "+line);
							if (line == null){ 
								break;
							}
						} catch (IOException e) {
							e.printStackTrace();
							break;
						}
					}
				}
			};
			standReadThread.start();

			errorReadThread=new Thread(){
				public void run(){
					InputStream is=p.getErrorStream();
					BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
					while (true){
						String line; 
						try {
							line = localBufferedReader.readLine();
							if (line == null){ 
								break;
							}else{ 
								//System.out.println("error "+line);
							}
						} catch (IOException e) {
							e.printStackTrace();
							//error();
							break;
						}
					}
				}
			};
			errorReadThread.start();
			standReadThread.join();
			errorReadThread.join();
			p.waitFor();
		} catch (Exception e) {
			e.printStackTrace();
			//error();
		}
	}

	public boolean isUseTcp() {
		return useTcp;
	}

}
