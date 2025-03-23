import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;

public class Client {


    private static final String DB_URL = "jdbc:mysql://112.74.60.165:3306/mydb?useSSL=false&serverTimezone=Asia/Shanghai";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "newPassword888!";
    private static final String SERVER_HOST = "112.74.60.165";
    private static final int SERVER_PORT = 8888;


    public static void main(String[] args) {
        try {
            Socket socket = new Socket(SERVER_HOST, SERVER_PORT);
            // Socket创建成功（可以连接服务器），开始输入用户名和密码进行登录
            Scanner mainScanner = new Scanner(System.in);
            System.out.println("请输入用户名：");
            String username = mainScanner.nextLine().trim();
            System.out.println("请输入密码：");
            String password = mainScanner.nextLine().trim();

            if (loginTest(username,password).next()) {
                System.out.println("登录成功");
                //修改在线状态
                updateOnlineStatus(username,true);

                OutputStream outputStream = socket.getOutputStream();
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                PrintWriter printWriter = new PrintWriter(outputStreamWriter);
                printWriter.println(username);// 发送1行（包括行结束符）
                printWriter.flush();

                // 启动2个线程，一个负责收、一个负责发
                Thread getMessage = new Thread(() -> getMessage(socket));
                getMessage.start();

                Thread sendMessage = new Thread(() -> sendMessage(username,socket,printWriter));
                sendMessage.start();

            } else {
                System.out.println("用户名或密码错误");
            }
        }
        catch (IOException | SQLException e) {
            throw new RuntimeException(e);
        }
    }



    //登录确认,返回一个结果集
    private static ResultSet loginTest(String username,String password){
        Connection connection;
        PreparedStatement statement;// 容易产生注入攻击
        String sql;
        ResultSet resultSet;

        try {
            sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(false);

//            statement = connection.createStatement();
            statement = connection.prepareStatement(sql);
            statement.setString(1, username);
            statement.setString(2, password);
            resultSet = statement.executeQuery();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return resultSet;
    }



    //更改数据库中在线状态
    private static void updateOnlineStatus(String username, boolean isOnline) {
        final String SQL = "UPDATE users SET is_online = ? WHERE username = ?";
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(SQL)) {
            stmt.setInt(1, isOnline ? 1 : 0);
            stmt.setString(2, username);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("状态更新失败: " + e.getMessage());
        }
    }



    //获取在线人数，输出在cmd中
    private static void getOnlineUsers(){
        List<String> names = new ArrayList<>();
        String sql = "SELECT username FROM users where is_online = 1";

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                names.add(rs.getString("username")); // 存入集合
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("在线用户：");
        names.forEach(System.out::println);
    }



    //收消息
    private static void getMessage(Socket finalSocket){
        while(true){
            try {
                InputStream inputStream=finalSocket.getInputStream();
                InputStreamReader inputStreamReader1=new InputStreamReader(inputStream);
                BufferedReader bufferedReader1 = new BufferedReader(inputStreamReader1);
                String msg=bufferedReader1.readLine();
                System.out.println("收到的消息："+msg);
                //接受消息

            } catch (IOException e) {
                System.err.println("已离线" + e.getMessage());
                break;
            }
        }
    }


    //发消息
    private static void sendMessage(String username, Socket socket, PrintWriter printWriter){
        while (true){

            //查询在线用户
            getOnlineUsers();

            Scanner scanner = new Scanner(System.in);
            System.out.println("请输入消息:格式为[目标|消息]，如：[ALL|你好],输入[离线]断开链接");
            String msg = scanner.nextLine();
            if (Objects.equals(msg, "离线")){
                try {
                    updateOnlineStatus(username,false);
                    socket.close();
                    return;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            printWriter.println(msg);
            printWriter.flush();
        }
    }

}