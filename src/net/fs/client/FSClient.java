package net.fs.client;

import net.fs.rudp.ClientProcessorInterface;
import net.fs.rudp.Route;
import org.pcap4j.core.PcapNativeException;
import org.pcap4j.core.Pcaps;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.util.HashSet;

class FSClient {

	Route route_udp,route_tcp;

	String serverAddress="";

	int serverPort=130;

	private HashSet<ClientProcessorInterface> processTable= new HashSet<>();

	private final Object syn_process=new Object();

	private boolean useTcp=true;

	public static void main(String[] args) {
		boolean tcpEnvSuccess=true;
		try {
			Pcaps.findAllDevs();
		} catch (PcapNativeException e) {
			tcpEnvSuccess=false;
			String msg = "启动失败,请先安装libpcap,否则无法使用tcp协议";
			System.out.println(msg);
		}

		try {
			new FSClient(tcpEnvSuccess,"103.217.253.73", 150, false);
		} catch (final Exception e1) {
			e1.printStackTrace();
			//System.exit(0);
		}
	}

	private FSClient(boolean tcpEnvSuccess, String serverAddress, int serverPort, boolean tcp) throws Exception {
		short routePort = -1234;//这个值在这里毫无意义
		route_tcp = new Route(null, routePort,Route.mode_client,true,tcpEnvSuccess);
		route_udp = new Route(null, routePort,Route.mode_client,false,tcpEnvSuccess);

		new PortMapManager(this);


		if(route_tcp.lastClientControl!=null){
			route_tcp.lastClientControl.close();
		}

		if(route_udp.lastClientControl!=null){
			route_udp.lastClientControl.close();
		}

		cleanTcpTunRule_linux();
		if(serverAddress!=null&&!serverAddress.equals("")){
			setFireWallRule(serverAddress,serverPort);
		}

		this.serverAddress=serverAddress;
		this.serverPort=serverPort;
		useTcp=tcp;

		if (!route_tcp.capEnv.tcpEnable) {
			System.out.println("无可用网络接口,只能使用udp协议.");
		}
		Route.localDownloadSpeed = 5957818;
		Route.localUploadSpeed = 476625;
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
	

	void onProcessClose(ClientProcessorInterface process){
		synchronized (syn_process) {
			processTable.remove(process);
		}
	}

	private static void runCommand(String command){
		Thread standReadThread;
		Thread errorReadThread;
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

	boolean isUseTcp() {
		return useTcp;
	}

}
