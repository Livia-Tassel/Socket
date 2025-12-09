package server;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * 聊天服务器主类
 * 负责监听客户端连接，管理所有在线客户端
 */
public class ChatServer {
    private static final int DEFAULT_PORT = 8888;
    
    private final int port;
    private ServerSocket serverSocket;
    private final ClientManager clientManager;
    private final GroupManager groupManager;
    private final MessageRouter messageRouter;
    private volatile boolean running;
    private final ExecutorService executorService;
    
    public ChatServer(int port) {
        this.port = port;
        this.clientManager = new ClientManager();
        this.groupManager = new GroupManager();
        this.messageRouter = new MessageRouter(clientManager, groupManager);
        this.executorService = Executors.newCachedThreadPool();
        this.running = false;
    }
    
    /**
     * 启动服务器
     */
    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            
            System.out.println("╔══════════════════════════════════════════════════════════╗");
            System.out.println("║          多人聊天系统服务器已启动                        ║");
            System.out.println("╠══════════════════════════════════════════════════════════╣");
            System.out.println("║  端口: " + port + "                                             ║");
            System.out.println("║  等待客户端连接...                                       ║");
            System.out.println("╚══════════════════════════════════════════════════════════╝");
            
            // 接受客户端连接
            while (running) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    System.out.println("[服务器] 新连接: " + clientSocket.getInetAddress().getHostAddress());
                    
                    // 为每个客户端创建处理器
                    ClientHandler handler = new ClientHandler(clientSocket, this);
                    executorService.execute(handler);
                    
                } catch (SocketException e) {
                    if (running) {
                        System.err.println("[服务器] 接受连接时出错: " + e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("[服务器] 启动失败: " + e.getMessage());
            e.printStackTrace();
        } finally {
            stop();
        }
    }
    
    /**
     * 停止服务器
     */
    public void stop() {
        running = false;
        
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.err.println("[服务器] 关闭服务器Socket时出错: " + e.getMessage());
        }
        
        // 断开所有客户端
        clientManager.disconnectAll();
        
        // 关闭线程池
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
        
        System.out.println("[服务器] 服务器已停止");
    }
    
    /**
     * 获取客户端管理器
     */
    public ClientManager getClientManager() {
        return clientManager;
    }
    
    /**
     * 获取群组管理器
     */
    public GroupManager getGroupManager() {
        return groupManager;
    }
    
    /**
     * 获取消息路由器
     */
    public MessageRouter getMessageRouter() {
        return messageRouter;
    }
    
    /**
     * 广播用户列表给所有在线用户
     */
    public void broadcastUserList() {
        List<String> users = clientManager.getOnlineUsernames();
        Message userListMsg = Message.createUserListMessage(users);
        clientManager.broadcast(userListMsg);
    }
    
    /**
     * 广播用户上线通知
     */
    public void broadcastUserJoin(String username) {
        Message joinMsg = Message.createUserJoinMessage(username);
        clientManager.broadcastExcept(joinMsg, username);
    }
    
    /**
     * 广播用户下线通知
     */
    public void broadcastUserLeave(String username) {
        Message leaveMsg = Message.createUserLeaveMessage(username);
        clientManager.broadcast(leaveMsg);
    }
    
    /**
     * 主程序入口
     */
    public static void main(String[] args) {
        int port = DEFAULT_PORT;
        
        // 解析命令行参数
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("无效的端口号，使用默认端口 " + DEFAULT_PORT);
            }
        }
        
        ChatServer server = new ChatServer(port);
        
        // 注册关闭钩子
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("\n[服务器] 正在关闭...");
            server.stop();
        }));
        
        server.start();
    }
}
