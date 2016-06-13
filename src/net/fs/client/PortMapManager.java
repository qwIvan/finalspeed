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

class PortMapManager {

    private FSClient FSClient;

    PortMapManager(FSClient FSClient) {
        this.FSClient = FSClient;
        //listenPort();
        loadMapRule();
    }

    private void loadMapRule() {
        String content;
        JSONObject json = null;
        try {
            String configFilePath = "port_map.json";
            content = readFileUtf8(configFilePath);
            json = JSONObject.parseObject(content);
        } catch (Exception e) {
            //e.printStackTrace();
        }
        if (json != null && json.containsKey("map_list")) {
            JSONArray json_map_list = json.getJSONArray("map_list");
            for (Object aJson_map_list : json_map_list) {
                JSONObject json_rule = (JSONObject) aJson_map_list;
                int listen_port = json_rule.getIntValue("listen_port");
                int dst_port = json_rule.getIntValue("dst_port");

                listen(listen_port,dst_port);
            }
        }
    }

    private void listen(int listen_port, int dst_port) {
        Route.es.execute(() -> {
            while (true) {
                try {
                    ServerSocket serverSocket = new ServerSocket(listen_port);
                    final Socket socket = serverSocket.accept();
                    Route.es.execute(() -> {
                        Route route = null;
                        if (FSClient.isUseTcp()) {
                            route = FSClient.route_tcp;
                        } else {
                            route = FSClient.route_udp;
                        }
                        new PortMapProcess(FSClient, route, socket, FSClient.serverAddress, FSClient.serverPort,
                                null, dst_port);
                    });

                } catch (IOException e) {
//                    e.printStackTrace();
                    break;
                }
            }
        });
    }

    private static String readFileUtf8(String path) throws Exception {
        String str = null;
        FileInputStream fis = null;
        DataInputStream dis = null;
        try {
            File file = new File(path);

            int length = (int) file.length();
            byte[] data = new byte[length];

            fis = new FileInputStream(file);
            dis = new DataInputStream(fis);
            dis.readFully(data);
            str = new String(data, "utf-8");

        } catch (Exception e) {
            //e.printStackTrace();
            throw e;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (dis != null) {
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
