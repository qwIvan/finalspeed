// Copyright (c) 2015 D1SM.net

package net.fs.rudp;

class ConnectException extends Exception{
	
	private static final long serialVersionUID = 8735513900170495107L;

	ConnectException(){
	}
	@Override
	public void printStackTrace(){
		//#System.out.println("连接异常 "+message);
	}

}
