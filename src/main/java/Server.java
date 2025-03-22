
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

// 每个同学都创建
public class Server {

    public static Map<String,Socket> map=new HashMap<>();
//    存储用户名以及相应的socket
    public static void main(String[] args) {
        ServerSocket serverSocket ;
        Socket socket ;
        try {
            serverSocket = new ServerSocket(8888);
//
//            // 读取server.properties，Java提供了相应类
//            Properties properties = new Properties();
//            properties.load(
//                    Server.class.getClassLoader().getResourceAsStream("server.properties")
//            );
//            String ip = properties.getProperty("ip");
//            String port = properties.getProperty("port");
//            System.out.println(ip);
//            System.out.println(port);
//            SendIpAndPort.sendIpAndPort(ip, Integer.parseInt(port));


            while (true){
                System.out.println("等待客户端连接......");
                socket = serverSocket.accept();

                InputStream inputStream = socket.getInputStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                String username = reader.readLine();
                System.out.println("用户名：" + username);
                map.put(username, socket);

                System.out.println("有新客户进入......");
                // 一直处于收消息状态，一旦收到消息就开始对消息进行解析
                GetMessageThread gmt = new GetMessageThread(reader,map,username);
                gmt.start();

            }

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}

class GetMessageThread extends Thread {

    private final BufferedReader reader;
    private final Map<String,Socket>map;
    private final String username;
    public GetMessageThread(BufferedReader reader,Map<String ,Socket>map,String username){
        this.reader=reader;
        this.map=map;
        this.username=username;
    }


    public void run() {
        String msg;
        try {
            while( (msg=reader.readLine())!=null){
                //解析收到的消息，获得username与消息
                //然后获取socket，发消息

                String[] strings= msg.split("\\|");
                //获取socket

                //若客户端发来的是“ALL”群发消息则遍历map中的username且获取其socket并发送消息

                if(Objects.equals(strings[0], "ALL")){

                    for (String key : map.keySet()){
                        Socket socket=map.get(key);
                        String flag="(群发)";
                        sendMsgWithTime(strings, socket,flag,username);
                    }
                }else{
                    Socket socket=map.get(strings[0]);
                    String flag="(私发)";
                    sendMsgWithTime(strings, socket,flag,username);
                }

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("收消息线程启动");
    }


    private void sendMsgWithTime(String[] strings, Socket socket,String flag,String username) {
        new Thread(()->{

            try {

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
                String formattedTime = LocalTime.now().format(formatter);

                OutputStream OutputStream = socket.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(OutputStream);
                PrintWriter printWriter=new PrintWriter(outputStreamWriter);
                printWriter.println(formattedTime+flag+username+":"+ strings[1]);
                printWriter.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

        }).start();
    }
}

//class SendMessageThread extends Thread {
//    public void run() {
//        System.out.println("发消息线程启动");
//    }
//}