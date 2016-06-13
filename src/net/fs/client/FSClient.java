// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.Route;
import org.pcap4j.core.Pcaps;

import java.io.*;

class FSClient {
    public static void main(String[] args) {
        new FSClient();
    }
    private MapClient mapClient;

    private ClientConfig config = null;

    static FSClient ui;

    private boolean b1 = false;

    FSClient() {
        ui = this;
        loadConfig();
        boolean tcpEnvSuccess=true;
        Thread thread = new Thread() {
            public void run() {
                try {
                    Pcaps.findAllDevs();
                    b1 = true;
                } catch (Exception e3) {
                    e3.printStackTrace();
                }
            }
        };
        thread.start();
        try {
            thread.join();
        } catch (InterruptedException e1) {
            e1.printStackTrace();
        }
        //JOptionPane.showMessageDialog(mainFrame,System.getProperty("os.name"));
        if (!b1) {
        	tcpEnvSuccess=false;
            String msg = "启动失败,请先安装libpcap,否则无法使用tcp协议";
            System.out.println(msg);
        }

        try {
            mapClient = new MapClient(this,tcpEnvSuccess);
        } catch (final Exception e1) {
            e1.printStackTrace();
            //System.exit(0);
        }

        if (!mapClient.route_tcp.capEnv.tcpEnable) {
            System.out.println("无可用网络接口,只能使用udp协议.");
        }

        mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getProtocal().equals("tcp"));


        setSpeed(config.getDownloadSpeed(), config.getUploadSpeed());
    }

    private void setSpeed(int downloadSpeed, int uploadSpeed) {
        config.setDownloadSpeed(downloadSpeed);
        config.setUploadSpeed(uploadSpeed);
        Route.localDownloadSpeed = downloadSpeed;
        Route.localUploadSpeed = config.uploadSpeed;
    }


    private void loadConfig() {
        ClientConfig cfg = new ClientConfig();
        String configFilePath = "client_config.json";
        if (!new File(configFilePath).exists()) {
            JSONObject json = new JSONObject();
            try {
                saveFile(json.toJSONString().getBytes(), configFilePath);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        try {
            String content = readFileUtf8(configFilePath);
            JSONObject json = JSONObject.parseObject(content);
            cfg.setServerAddress(json.getString("server_address"));
            cfg.setServerPort(json.getIntValue("server_port"));
            cfg.setRemotePort(json.getIntValue("remote_port"));
            cfg.setDownloadSpeed(json.getIntValue("download_speed"));
            cfg.setUploadSpeed(json.getIntValue("upload_speed"));
            if (json.containsKey("protocal")) {
                cfg.setProtocal(json.getString("protocal"));
            }
            if (json.containsKey("recent_address_list")) {
            	JSONArray list=json.getJSONArray("recent_address_list");
                for (Object aList : list) {
                    cfg.getRecentAddressList().add(aList.toString());
                }
            }
           
            config = cfg;
        } catch (Exception e) {
            e.printStackTrace();
        }
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
            e.printStackTrace();
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

    private void saveFile(byte[] data, String path) throws Exception {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(path);
            fos.write(data);
        } catch (Exception e) {
            System.out.println("保存配置文件失败,请尝试以管理员身份运行! " + path);
            System.exit(0);
            throw e;
        } finally {
            if (fos != null) {
                fos.close();
            }
        }
    }

}
