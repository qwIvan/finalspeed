// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

class TrafficEvent {

	static int type_downloadTraffic=10;
	
	static int type_uploadTraffic=11;
	
	private int type=type_downloadTraffic;

	TrafficEvent(int traffic, int type){
		int traffic1 = traffic;
		this.type=type;
	}

	public int getType() {
		return type;
	}


}
