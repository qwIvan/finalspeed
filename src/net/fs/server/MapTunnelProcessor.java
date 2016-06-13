// Copyright (c) 2015 D1SM.net

package net.fs.server;

import com.alibaba.fastjson.JSONObject;
import net.fs.client.Pipe;
import net.fs.rudp.*;
import net.fs.utils.MLog;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;


public class MapTunnelProcessor implements ConnectionProcessor{

	private Socket dstSocket=null;

	private boolean closed=false;

	private ConnectionUDP conn;


	private UDPInputStream  tis;

	private UDPOutputStream tos;

	private InputStream sis;

	private OutputStream sos;

	public void process(final ConnectionUDP conn){
		this.conn=conn;
		Route.es.execute(this::process);
	}


	private void process(){

		tis=conn.uis;
		tos=conn.uos;

		byte[] headData;
		try {
			headData = tis.read2();
			String hs=new String(headData,"utf-8");
			JSONObject requestJSon=JSONObject.parseObject(hs);
			final int dstPort=requestJSon.getIntValue("dst_port");
			String message="";
			JSONObject responeJSon=new JSONObject();
			int code;
			code=Constant.code_success;
			responeJSon.put("code", code);
			responeJSon.put("message", message);
			byte[] responeData=responeJSon.toJSONString().getBytes("utf-8");
			tos.write(responeData, 0, responeData.length);
			if(code!=Constant.code_success){
				close();
				return;
			}
			dstSocket = new Socket("127.0.0.1", dstPort);
			dstSocket.setTcpNoDelay(true);
			sis=dstSocket.getInputStream();
			sos=dstSocket.getOutputStream();

			final Pipe p1=new Pipe();
			final Pipe p2=new Pipe();

			Route.es.execute(() -> {
                try {
                    p1.pipe(sis, tos);
                }catch (Exception e) {
                    //e.printStackTrace();
                }finally{
                    close();
                    if(p1.getReadedLength()==0){
                        MLog.println("端口"+dstPort+"无返回数据");
                    }
                }
            });
			Route.es.execute(() -> {
                try {
                    p2.pipe(tis,sos);
                }catch (Exception e) {
                    //e.printStackTrace();
                }finally{
                    close();
                }
            });


		} catch (Exception e2) {
			//e2.printStackTrace();
			close();
		}



	}

	private void close(){
		if(!closed){
			closed=true;
			if(sis!=null){
				try {
					sis.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
			if(sos!=null){
				try {
					sos.close();
				} catch (IOException e) {
					//e.printStackTrace();
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
			if(dstSocket!=null){
				try {
					dstSocket.close();
				} catch (IOException e) {
					//e.printStackTrace();
				}
			}
		}
	}

}
