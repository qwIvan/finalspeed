// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

public class SendRecord {
	
	private int sendSize;
	private int sendSize_First;
	
	private int sendCount;
	
	private int ackedSize;

	private int resended;
	
	SendRecord(){
		
	}

	void addResended(int size){
		resended+=size;
	}
	
	void addSended(int size){
		sendCount++;
		sendSize+=size;
	}
	
	void addSended_First(int size){
		sendSize_First+=size;
	}

	public int getSendSize() {
		return sendSize;
	}

	public int getAckedSize() {
		return ackedSize;
	}

	//接收到的数据大小
	public void setAckedSize(int ackedSize) {
		if(ackedSize>this.ackedSize){
			this.ackedSize = ackedSize;
		}
	}

	public void setTimeId(int timeId) {
		int timeId1 = timeId;
	}

}
