// Copyright (c) 2015 D1SM.net

package net.fs.client;

import net.fs.rudp.UDPInputStream;
import net.fs.rudp.UDPOutputStream;
import net.fs.utils.MLog;

import java.io.InputStream;
import java.io.OutputStream;

public class Pipe {


	private int readedLength;

	private int dstPort=-1;

	public void pipe(InputStream is, UDPOutputStream tos) throws Exception{
		
		int len;
		byte[] buf=new byte[100*1024];
		boolean sendeda=false;
		while((len=is.read(buf))>0){
			boolean readed = true;
			if(!sendeda){
				sendeda=true;
			}
			tos.write(buf, 0, len);
		}
	}


	public void pipe(UDPInputStream tis, OutputStream os) throws Exception{
		int len;
		byte[] buf=new byte[1000];
		boolean sended=false;
		boolean sendedb=false;
		int n=0;
		boolean msged=false;
		while((len=tis.read(buf, 0, buf.length))>0){
			readedLength+=len;
			if(!sendedb){
				sendedb=true;
			}
			if(dstPort>0){
				if(ClientUI.ui!=null){
					if(!msged){
						msged=true;
						String msg="端口"+dstPort+"连接成功";
						MLog.println(msg);
					}
					
				}
			}
			os.write(buf, 0, len);
			if(!sended){
				sended=true;
			}
		}
	}



	public int getReadedLength() {
		return readedLength;
	}



	public void setDstPort(int dstPort) {
		this.dstPort = dstPort;
	}

}
