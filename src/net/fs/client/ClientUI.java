// Copyright (c) 2015 D1SM.net

package net.fs.client;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import net.fs.rudp.Route;
import net.fs.utils.LogOutputStream;
import net.fs.utils.MLog;
import org.pcap4j.core.Pcaps;

import java.io.*;

class ClientUI implements ClientUII {
    private MapClient mapClient;

    private ClientConfig config = null;

    public static ClientUI ui;

    //    MapRuleListModel model;

//    public MapRuleListTable tcpMapRuleListTable;

    private boolean b1 = false;

    private boolean success_firewall_windows = true;

    private boolean success_firewall_osx = true;

    private String systemName = null;

    private boolean osx_fw_pf = false;

    private boolean osx_fw_ipfw = false;


    ClientUI(final boolean isVisible) {
        if(isVisible){
            LogOutputStream los = new LogOutputStream(System.out);
             System.setOut(los);
             System.setErr(los);
        }

        systemName = System.getProperty("os.name").toLowerCase();
        MLog.info("System: " + systemName + " " + System.getProperty("os.version"));
        ui = this;

        checkQuanxian();
        loadConfig();

        //mainFrame.setSize(width, 380);


        boolean tcpEnvSuccess=true;
        checkFireWallOn();
        if (!success_firewall_windows) {
        	tcpEnvSuccess=false;
            MLog.println("启动windows防火墙失败,请先运行防火墙服务.");
           // System.exit(0);
        }
        if (!success_firewall_osx) {
        	tcpEnvSuccess=false;
            MLog.println("启动ipfw/pfctl防火墙失败,请先安装.");
            //System.exit(0);
        }

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
            if (systemName.contains("windows")) {
                msg = "启动失败,请先安装winpcap,否则无法使用tcp协议";
            }

            MLog.println(msg);
            if (systemName.contains("windows")) {
                try {
                    Runtime.getRuntime().exec("winpcap_install.exe", null);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //System.exit(0);
            }
        }

        try {
            mapClient = new MapClient(this,tcpEnvSuccess);
        } catch (final Exception e1) {
            e1.printStackTrace();
            //System.exit(0);
        }

        if (!mapClient.route_tcp.capEnv.tcpEnable) {
            MLog.println("无可用网络接口,只能使用udp协议.");
        }

        mapClient.setUi(this);

        mapClient.setMapServer(config.getServerAddress(), config.getServerPort(), config.getProtocal().equals("tcp")
        );


        setSpeed(config.getDownloadSpeed(), config.getUploadSpeed());
    }

    private void checkFireWallOn() {
        if (systemName.contains("os x")) {
            String runFirewall = "ipfw";
            try {
                final Process p = Runtime.getRuntime().exec(runFirewall, null);
                osx_fw_ipfw = true;
            } catch (IOException e) {
                //e.printStackTrace();
            }
            runFirewall = "pfctl";
            try {
                final Process p = Runtime.getRuntime().exec(runFirewall, null);
                osx_fw_pf = true;
            } catch (IOException e) {
               // e.printStackTrace();
            }
            success_firewall_osx = osx_fw_ipfw | osx_fw_pf;
        } else if (systemName.contains("windows")) {
            String runFirewall = "netsh advfirewall set allprofiles state on";
            Thread standReadThread = null;
            Thread errorReadThread = null;
            try {
                final Process p = Runtime.getRuntime().exec(runFirewall, null);
                standReadThread = new Thread() {
                    public void run() {
                        InputStream is = p.getInputStream();
                        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                        while (true) {
                            String line;
                            try {
                                line = localBufferedReader.readLine();
                                if (line == null) {
                                    break;
                                } else {
                                    if (line.contains("Windows")) {
                                        success_firewall_windows = false;
                                    }
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                //error();
                                exit();
                                break;
                            }
                        }
                    }
                };
                standReadThread.start();

                errorReadThread = new Thread() {
                    public void run() {
                        InputStream is = p.getErrorStream();
                        BufferedReader localBufferedReader = new BufferedReader(new InputStreamReader(is));
                        while (true) {
                            String line;
                            try {
                                line = localBufferedReader.readLine();
                                if (line == null) {
                                    break;
                                } else {
                                    System.out.println("error" + line);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                                //error();
                                exit();
                                break;
                            }
                        }
                    }
                };
                errorReadThread.start();
            } catch (IOException e) {
                e.printStackTrace();
                success_firewall_windows = false;
                //error();
            }

            if (standReadThread != null) {
                try {
                    standReadThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            if (errorReadThread != null) {
                try {
                    errorReadThread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    private void checkQuanxian() {
        if (systemName.contains("windows")) {
            boolean b;
            File file = new File(System.getenv("WINDIR") + "\\test.file");
            //System.out.println("kkkkkkk "+file.getAbsolutePath());
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            b = file.exists();
            file.delete();

            if (!b) {
                MLog.println("请以管理员身份运行,否则可能无法正常工作! ");
//                System.exit(0);
            }
        }
    }

    private void setSpeed(int downloadSpeed, int uploadSpeed) {
        config.setDownloadSpeed(downloadSpeed);
        config.setUploadSpeed(uploadSpeed);
        Route.localDownloadSpeed = downloadSpeed;
        Route.localUploadSpeed = config.uploadSpeed;
    }


    private void exit() {
        System.exit(0);
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
            MLog.println("保存配置文件失败,请尝试以管理员身份运行! " + path);
            System.exit(0);
            throw e;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (Exception ignored){
                }
            }
        }
    }


    public boolean isOsx_fw_pf() {
        return osx_fw_pf;
    }

    public boolean isOsx_fw_ipfw() {
        return osx_fw_ipfw;
    }

}
