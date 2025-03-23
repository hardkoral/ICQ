import java.io.*;
import java.net.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.concurrent.*;

public class Server {
    // 使用线程安全的ConcurrentHashMap
    public static final Map<String, Socket> onlineUsers = new HashMap<>();
    // 发送消息的线程池
    private static final ExecutorService sendThreadPool = Executors.newCachedThreadPool();

    public static void main(String[] args) {

        try (ServerSocket serverSocket = new ServerSocket(8888)) {
            System.out.println("服务器已启动");

            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("检测到新客户端连接...");

                // 为新客户端启动处理线程
                new Thread(() -> handleClient(clientSocket)).start();
            }
        } catch (IOException e) {
            System.err.println("服务器启动失败: " + e.getMessage());
        }
    }

    private static void handleClient(Socket clientSocket) {
        String username = null;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(clientSocket.getInputStream()))) {

            // 读取用户名
            username = reader.readLine();
            if (username == null) {
                return; // 客户端未发送用户名直接断开
            }

            onlineUsers.put(username, clientSocket);
            System.out.println("用户 [" + username + "] 加入聊天室");

            // 持续接收消息
            String message;
            while ((message = reader.readLine()) != null) {
                processMessage(message, username, clientSocket);
            }
        } catch (IOException e) {
            System.out.println("用户 [" + username + "] 断开连接");
        } finally {
            cleanupUser(username, clientSocket);
        }
    }

    private static void processMessage(String message, String sender, Socket senderSocket) {
        String[] parts = message.split("\\|", 2); // 限制分割次数为2
        if (parts.length < 2) {
            sendError(senderSocket, "无效消息格式，请使用 [目标|内容]");
            return;
        }

        String target = parts[0];
        String content = parts[1];

        if ("ALL".equalsIgnoreCase(target)) {
            broadcastMessage(sender, content);
        } else {
            sendPrivateMessage(sender, target, content, senderSocket);
        }
        System.out.println("["+sender+"] to ["+target+"]:"+content);

    }

    // 群发消息
    private static void broadcastMessage(String sender, String content) {
        String formattedMessage = formatMessage(sender, content, "(群发)");
        onlineUsers.forEach((user, socket) -> {
            if (!user.equals(sender)) { // 不发送给自己
                sendMessage(socket, formattedMessage);
            }
        });
    }

    // 私发消息
    private static void sendPrivateMessage(String sender, String target, String content, Socket senderSocket) {
        Socket targetSocket = onlineUsers.get(target);
        if (targetSocket != null && !targetSocket.isClosed()) {
            String formattedMessage = formatMessage(sender, content, "(私发)");
            sendMessage(targetSocket, formattedMessage);
        } else {
            sendError(senderSocket, "错误: 用户 [" + target + "] 不存在或已离线");
        }
    }

    // 发送消息（线程池处理）
    private static void sendMessage(Socket socket, String message) {
        sendThreadPool.submit(() -> {
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(message);
            } catch (IOException e) {
                System.err.println("消息发送失败: " + e.getMessage());
            }
        });
    }

    // 格式化消息（含时间戳）
    private static String formatMessage(String sender, String content, String flag) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        return LocalTime.now().format(formatter) + flag + sender + ": " + content;
    }

    // 发送错误提示给发送者
    private static void sendError(Socket socket, String errorMsg) {
        sendThreadPool.submit(() -> {
            try {
                PrintWriter writer = new PrintWriter(socket.getOutputStream(), true);
                writer.println(errorMsg);
            } catch (IOException e) {
                System.err.println("发送错误消息失败: " + e.getMessage());
            }
        });
    }


    // 清理离线用户
    private static void cleanupUser(String username, Socket socket) {
        if (username != null) {
            onlineUsers.remove(username);
            System.out.println("用户 [" + username + "] 已从在线列表移除");
        }
        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.err.println("关闭Socket失败: " + e.getMessage());
        }
    }
}