// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.io.Serializable;

class MapRule implements Serializable{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -3504577683070928480L;

	int listen_port;
	
	int dst_port;

	int getListen_port() {
		return listen_port;
	}

}
