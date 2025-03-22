import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SendIpAndPort {

    public static void sendIpAndPort(String ip, int port) {
        // 发送IP和端口，其实就是把IP和端口存到数据库（统一使用老师的或某一个同学的MySQL）
        // 使用Java执行INSERT语句
        String url = "jdbc:mysql://39.108.123.201:3306/mydb?useSSL=false&useUnicode=true&characterEncoding=utf-8&serverTimezone=Asia/Shanghai";
        String username = "root";
        String password = "GuetLq18Ban666!&";
        Connection connection = null;
        PreparedStatement preparedStatement = null;
        String sql = "INSERT INTO users(ip,port) VALUES(?,?)";
        try {
            connection = DriverManager.getConnection(url, username, password);
            connection.setAutoCommit(false);
            preparedStatement = connection.prepareStatement(sql);
            preparedStatement.setString(1, ip);
            preparedStatement.setInt(2, port);

            preparedStatement.executeUpdate();
            connection.commit();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                preparedStatement.close();
                connection.close();
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
