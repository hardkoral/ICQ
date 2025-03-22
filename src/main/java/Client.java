import java.io.*;
import java.net.Socket;
import java.sql.*;
import java.util.Scanner;

public class Client {
    public static void main(String[] args) {
        String url = "jdbc:mysql://112.74.60.165:3306/mydb?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        Socket socket = null;
        Connection connection = null;
        PreparedStatement statement = null;// 容易产生注入攻击
        String sql = "";
        ResultSet resultSet = null;
        try {
            socket = new Socket("localhost", 8888);
            // Socket创建成功（可以连接服务器），开始输入用户名和密码进行登录

            InputStreamReader inputStreamReader = new InputStreamReader(System.in);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

            System.out.println("请输入用户名：");
            String username = bufferedReader.readLine();
            System.out.println("请输入密码：");
            String password = bufferedReader.readLine();

            // 连接数据库，验证我们输入的用户名和密码，其实就是用Java程序去执行下面的这条SQL语句
            // SELECT * FROM users WHERE username='liwei' AND password='lw12345'
            // 拼SQL（把username和password变量拼接到一起）
//            sql = "SELECT * FROM users WHERE username='" + username + "' AND password='" + password + "'";
            sql = "SELECT * FROM users WHERE username = ? AND password = ?";
            connection = DriverManager.getConnection(url, "root", "newPassword888!");
            connection.setAutoCommit(false);

//            statement = connection.createStatement();
            statement = connection.prepareStatement(sql);

            statement.setString(1, username);
            statement.setString(2, password);

            resultSet = statement.executeQuery();
            // 接下来就可以从resultSet中获取数据
            // 如果可以查询到数据，说明：用户名和密码是正确的
            if (resultSet.next()) {
                System.out.println("登录成功");
                // 修改状态（是否在线）
                // 执行UPDATE users SET is_online =1 WHERE username=?
                sql = "UPDATE users SET is_online =1 WHERE username=?";
                statement = connection.prepareStatement(sql);
                statement.setString(1, username);
                statement.executeUpdate();
                connection.commit();

                // 发送用户名给Server（备案）
                OutputStream outputStream = socket.getOutputStream();
                //Reader
                //XXXWriter
                OutputStreamWriter outputStreamWriter = new OutputStreamWriter(outputStream);
                PrintWriter printWriter = new PrintWriter(outputStreamWriter);
                printWriter.println(username);// 发送1行（包括行结束符）
                printWriter.flush();
                // 启动2个线程，一个负责收、一个负责发
                Socket finalSocket = socket;
                Thread getMessage = new Thread(() -> {
                    while(true){
                        try {
                            InputStream inputStream=finalSocket.getInputStream();
                            InputStreamReader inputStreamReader1=new InputStreamReader(inputStream);
                            BufferedReader bufferedReader1 = new BufferedReader(inputStreamReader1);
                            String msg=bufferedReader1.readLine();
                            System.out.println("收到的消息："+msg);
                            //接受消息

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }
                });
                getMessage.start();

                Thread sendMessage = new Thread(() -> {
                    Scanner scanner = new Scanner(System.in);
                    System.out.println("请输入消息:格式为[目标|消息]，如：[ALL|你好]");
                    String msg = scanner.nextLine();

                    printWriter.println(msg);
                    printWriter.flush();
                });
                sendMessage.start();
            } else {
                System.out.println("用户名或密码错误");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}