// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.Route;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;

class PortMapManager {
	
	private MapClient mapClient;
	
	private ArrayList<MapRule> mapList= new ArrayList<>();
	
	private HashMap<Integer, MapRule> mapRuleTable= new HashMap<>();

	PortMapManager(MapClient mapClient){
		this.mapClient=mapClient;
		//listenPort();
		loadMapRule();
	}

	private void loadMapRule(){
		String content;
		JSONObject json=null;
		try {
			String configFilePath = "port_map.json";
			content = readFileUtf8(configFilePath);
			json=JSONObject.parseObject(content);
		} catch (Exception e) {
			//e.printStackTrace();
		}
		if(json!=null&&json.containsKey("map_list")){
			JSONArray json_map_list=json.getJSONArray("map_list");
			for (Object aJson_map_list : json_map_list) {
				JSONObject json_rule = (JSONObject) aJson_map_list;
				MapRule mapRule = new MapRule();
				mapRule.listen_port = json_rule.getIntValue("listen_port");
				mapRule.dst_port = json_rule.getIntValue("dst_port");
				mapList.add(mapRule);
				ServerSocket serverSocket;
				try {
					serverSocket = new ServerSocket(mapRule.getListen_port());
					listen(serverSocket);
				} catch (IOException e) {
					e.printStackTrace();
				}
				mapRuleTable.put(mapRule.listen_port, mapRule);
			}
		}

	}

	private void listen(final ServerSocket serverSocket){
		Route.es.execute(() -> {
            while(true){
                try {
                    final Socket socket=serverSocket.accept();
                    Route.es.execute(() -> {
int listenPort=serverSocket.getLocalPort();
MapRule mapRule=mapRuleTable.get(listenPort);
if(mapRule!=null){
Route route=null;
if(mapClient.isUseTcp()){
route=mapClient.route_tcp;
}else {
route=mapClient.route_udp;
}
PortMapProcess process=new PortMapProcess(mapClient,route, socket,mapClient.serverAddress,mapClient.serverPort,
null,mapRule.dst_port);
}
});

                } catch (IOException e) {
                    e.printStackTrace();
                    break;
                }
            }
        });
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
			//e.printStackTrace();
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
}
