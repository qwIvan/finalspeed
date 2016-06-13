// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.*;
import net.fs.utils.MLog;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;

class PortMapProcess implements ClientProcessorInterface{

	private UDPInputStream  tis;

	private UDPOutputStream tos;

	private ConnectionUDP conn;

	private MapClient mapClient;

	private Socket srcSocket;

	private DataInputStream srcIs=null;
	private DataOutputStream srcOs=null;

	private boolean closed=false;

	public PortMapProcess(MapClient mapClient, Route route, final Socket srcSocket, String serverAddress2, int serverPort2,
						  String dstAddress, final int dstPort){
		this.mapClient=mapClient;

		this.srcSocket=srcSocket;

		try {
			srcIs = new DataInputStream(srcSocket.getInputStream());
			srcOs=new DataOutputStream(srcSocket.getOutputStream());
			conn = route.getConnection(serverAddress2, serverPort2);
			tis=conn.uis;
			tos=conn.uos;

			JSONObject requestJson=new JSONObject();
			requestJson.put("dst_address", dstAddress);
			requestJson.put("dst_port", dstPort);
			byte[] requestData=requestJson.toJSONString().getBytes("utf-8");
			
			tos.write(requestData, 0, requestData.length);


			final Pipe p1=new Pipe();
			final Pipe p2=new Pipe();


			byte[] responeData=tis.read2();

			String hs=new String(responeData,"utf-8");
			JSONObject responeJSon=JSONObject.parseObject(hs);
			int code=responeJSon.getIntValue("code");
			String message=responeJSon.getString("message");
			String uimessage;
			if(code==Constant.code_success){

				Route.es.execute(() -> {
                    long t=System.currentTimeMillis();
                    p2.setDstPort(dstPort);
                    try {
                        p2.pipe(tis, srcOs);
                    }catch (Exception e) {
                        e.printStackTrace();
                    }finally{
                        close();
                        if(p2.getReadedLength()==0){
                            //String msg="fs服务连接成功,加速端口"+dstPort+"连接失败1";
                            String msg="端口"+dstPort+"无返回数据";
                            MLog.println(msg);
                        }
                    }
                });

				Route.es.execute(() -> {
                    try {
                        p1.pipe(srcIs, tos);
                    } catch (Exception e) {
                        //e.printStackTrace();
                    }finally{
                        close();
                    }
                });
			}else {
				close();
				uimessage="fs服务连接成功,端口"+dstPort+"连接失败2";
				MLog.println(uimessage);
			}
		} catch (Exception e1) {
			e1.printStackTrace();
			String msg="fs服务连接失败!";
			MLog.println(msg);
		}

	}

	private void close(){
		if(!closed){
			closed=true;
			if(srcIs!=null){
				try {
					srcIs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(srcOs!=null){
				try {
					srcOs.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(tos!=null){
				tos.closeStream_Local();
			}
			if(tis!=null){
				tis.closeStream_Local();
			}
			if(conn!=null){
				conn.close_local();
			}
			if(srcSocket!=null){
				try {
					srcSocket.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			mapClient.onProcessClose(this);

		}
	}

}
