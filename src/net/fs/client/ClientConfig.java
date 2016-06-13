// Copyright (c) 2015 D1SM.net

package net.fs.client;

import java.util.ArrayList;

class ClientConfig {
	
	private String serverAddress="";
	
	private int serverPort;

	int downloadSpeed,uploadSpeed;

	private String protocal="tcp";

	private ArrayList<String> recentAddressList= new ArrayList<>();

	public String getServerAddress() {
		return serverAddress;
	}

	public void setServerAddress(String serverAddress) {
		this.serverAddress = serverAddress;
	}

	public int getServerPort() {
		return serverPort;
	}

	public void setServerPort(int serverPort) {
		this.serverPort = serverPort;
	}

	public void setRemotePort(int remotePort) {
		int remotePort1 = remotePort;
	}

	public int getDownloadSpeed() {
		return downloadSpeed;
	}

	public void setDownloadSpeed(int downloadSpeed) {
		this.downloadSpeed = downloadSpeed;
	}

	public int getUploadSpeed() {
		return uploadSpeed;
	}

	public void setUploadSpeed(int uploadSpeed) {
		this.uploadSpeed = uploadSpeed;
	}

	public String getProtocal() {
		return protocal;
	}

	public void setProtocal(String protocal) {
		this.protocal = protocal;
	}

	public ArrayList<String> getRecentAddressList() {
		return recentAddressList;
	}

}
