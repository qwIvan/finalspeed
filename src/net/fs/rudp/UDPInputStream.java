// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public class UDPInputStream {

	private Receiver receiver;

	UDPInputStream(ConnectionUDP conn){
		receiver=conn.receiver;
	}
	
	public int read(byte[] b, int off, int len) throws ConnectException, InterruptedException {
		byte[] b2=null;
		b2 = read2();
		if(len<b2.length){
			throw new ConnectException();
		}else{
			System.arraycopy(b2, 0, b, off, b2.length);
			return b2.length;
		}
	}
	
	public byte[] read2() throws ConnectException, InterruptedException{
		return receiver.receive();
	}
	
	public void closeStream_Local(){
		boolean streamClosed = false;
		if(!streamClosed){
			receiver.closeStream_Local();
		}
	}


}
