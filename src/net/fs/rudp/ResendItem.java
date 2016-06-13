// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

class ResendItem {
	
	private int count;
	
	ConnectionUDP conn;
	
	int sequence;
	
	private long resendTime;
	
	ResendItem(ConnectionUDP conn,int sequence){
		this.conn=conn;
		this.sequence=sequence;
	}
	
	void addCount(){
		count++;
	}

	public int getCount() {
		return count;
	}

	public long getResendTime() {
		return resendTime;
	}

	public void setResendTime(long resendTime) {
		this.resendTime = resendTime;
	}

}
