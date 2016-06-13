// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.Route;

import java.io.*;

class ClientNoUI{

	private ClientConfig config;

	ClientNoUI(){
		loadConfig();
		Route.localDownloadSpeed=config.downloadSpeed;
		Route.localUploadSpeed=config.uploadSpeed;
//		mapClient=new MapClient(config.getSocks5Port());
//		mapClient.setUi(this);
//		mapClient.setMapServer(config.getServerAddress(), config.getServerPort(),config.getRemotePort()	,config.getPasswordMd5(),config.getPasswordMd5_Proxy(),config.isDirect_cn());
	}

	private void loadConfig(){
		ClientConfig cfg=new ClientConfig();
		String configFilePath = "client_config.json";
		if(!new File(configFilePath).exists()){
			JSONObject json=new JSONObject();
			try {
				saveFile(json.toJSONString().getBytes(), configFilePath);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		try {
			String content=readFileUtf8(configFilePath);
			JSONObject json=JSONObject.parseObject(content);
			cfg.setServerAddress(json.getString("server_address"));
			cfg.setServerPort(json.getIntValue("server_port"));
			cfg.setRemotePort(json.getIntValue("remote_port"));
			cfg.setDownloadSpeed(json.getIntValue("download_speed"));
			cfg.setUploadSpeed(json.getIntValue("upload_speed"));
			config=cfg;
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
		
	private static String readFileUtf8(String path) throws Exception{
		String str=null;
		FileInputStream fis=null;
		DataInputStream dis=null;
		try {
			File file=new File(path);

			int length=(int) file.length();
			byte[] data=new byte[length];

			fis=new FileInputStream(file);
			dis=new DataInputStream(fis);
			dis.readFully(data);
			str=new String(data,"utf-8");

		} catch (Exception e) {
			e.printStackTrace();
			throw e;
		}finally{
			if(fis!=null){
				try {
					fis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(dis!=null){
				try {
					dis.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		return str;
	}
	
	private void saveFile(byte[] data, String path) throws Exception{
		FileOutputStream fos=null;
		try {
			fos=new FileOutputStream(path);
			fos.write(data);
		} finally {
			if(fos!=null){
				try {
					fos.close();
				} catch (Exception ignored) {
				}
			}
		}
	}

}
