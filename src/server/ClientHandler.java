package server;

import common.*;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 客户端连接处理器
 * 负责处理单个客户端的消息收发
 */
public class ClientHandler implements Runnable {
    private final Socket socket;
    private final ChatServer server;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private volatile boolean connected;
    
    public ClientHandler(Socket socket, ChatServer server) {
        this.socket = socket;
        this.server = server;
        this.connected = true;
    }
    
    @Override
    public void run() {
        try {
            // 初始化输入输出流
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            // 处理消息循环
            String line;
            while (connected && (line = reader.readLine()) != null) {
                try {
                    Message message = JsonUtils.fromJson(line);
                    handleMessage(message);
                } catch (Exception e) {
                    System.err.println("[处理器] 解析消息出错: " + e.getMessage());
                    sendMessage(Message.createErrorMessage("消息格式错误"));
                }
            }
        } catch (IOException e) {
            if (connected) {
                System.err.println("[处理器] 连接异常: " + e.getMessage());
            }
        } finally {
            disconnect();
        }
    }
    
    /**
     * 处理接收到的消息
     */
    private void handleMessage(Message message) {
        System.out.println("[处理器] 收到消息: " + message.getType() + " from " + 
                          (username != null ? username : "未登录用户"));
        
        switch (message.getType()) {
            case LOGIN:
                handleLogin(message);
                break;
            case LOGOUT:
                handleLogout();
                break;
            case TEXT:
            case IMAGE:
            case FILE:
            case FILE_DATA:
                handleDataMessage(message);
                break;
            case CREATE_GROUP:
                handleCreateGroup(message);
                break;
            case LEAVE_GROUP:
                handleLeaveGroup(message);
                break;
            case HEARTBEAT:
                // 心跳包，不需要处理
                break;
            default:
                sendMessage(Message.createErrorMessage("不支持的消息类型: " + message.getType()));
        }
    }
    
    /**
     * 处理登录请求
     */
    private void handleLogin(Message message) {
        String requestedUsername = message.getContentString("username");
        
        if (requestedUsername == null || requestedUsername.trim().isEmpty()) {
            sendMessage(Message.createLoginResponse(false, "用户名不能为空"));
            return;
        }
        
        requestedUsername = requestedUsername.trim();
        
        // 检查用户名是否已被使用
        if (server.getClientManager().isUserOnline(requestedUsername)) {
            sendMessage(Message.createLoginResponse(false, "用户名已被使用"));
            return;
        }
        
        // 登录成功
        this.username = requestedUsername;
        server.getClientManager().addClient(username, this);
        
        // 发送登录成功响应
        sendMessage(Message.createLoginResponse(true, "登录成功"));
        
        // 发送当前在线用户列表
        List<String> users = server.getClientManager().getOnlineUsernames();
        sendMessage(Message.createUserListMessage(users));
        
        // 发送群组列表
        List<Group> groups = server.getGroupManager().getGroupsForUser(username);
        sendMessage(Message.createGroupListMessage(groups));
        
        // 广播新用户上线
        server.broadcastUserJoin(username);
        
        System.out.println("[处理器] 用户登录成功: " + username);
    }
    
    /**
     * 处理登出请求
     */
    private void handleLogout() {
        disconnect();
    }
    
    /**
     * 处理数据消息（文字、图片、文件）
     */
    private void handleDataMessage(Message message) {
        if (username == null) {
            sendMessage(Message.createErrorMessage("请先登录"));
            return;
        }
        
        // 确保发送者是当前用户
        message.setSender(username);
        
        // 路由消息
        server.getMessageRouter().routeMessage(message);
    }
    
    /**
     * 处理创建群组请求
     */
    @SuppressWarnings("unchecked")
    private void handleCreateGroup(Message message) {
        if (username == null) {
            sendMessage(Message.createErrorMessage("请先登录"));
            return;
        }
        
        String groupName = message.getContentString("groupName");
        Object membersObj = message.getContent().get("members");
        
        if (groupName == null || groupName.trim().isEmpty()) {
            sendMessage(Message.createErrorMessage("群组名称不能为空"));
            return;
        }
        
        List<String> members = new ArrayList<>();
        if (membersObj instanceof List) {
            for (Object m : (List<?>) membersObj) {
                members.add(m.toString());
            }
        }
        
        // 确保创建者在成员列表中
        if (!members.contains(username)) {
            members.add(username);
        }
        
        // 创建群组
        Group group = server.getGroupManager().createGroup(groupName, username, members);
        
        // 通知所有群组成员
        Message groupCreatedMsg = Message.createGroupCreatedMessage(group);
        for (String member : members) {
            ClientHandler handler = server.getClientManager().getClient(member);
            if (handler != null) {
                handler.sendMessage(groupCreatedMsg);
            }
        }
        
        System.out.println("[处理器] 创建群组: " + groupName + ", 成员: " + members);
    }
    
    /**
     * 处理离开群组请求
     */
    private void handleLeaveGroup(Message message) {
        if (username == null) {
            sendMessage(Message.createErrorMessage("请先登录"));
            return;
        }
        
        String groupId = message.getContentString("groupId");
        if (groupId != null) {
            server.getGroupManager().removeMemberFromGroup(groupId, username);
        }
    }
    
    /**
     * 发送消息给客户端
     */
    public void sendMessage(Message message) {
        if (writer != null && connected) {
            String json = JsonUtils.toJson(message);
            writer.println(json);
        }
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        // 从客户端管理器中移除
        if (username != null) {
            server.getClientManager().removeClient(username);
            server.broadcastUserLeave(username);
            System.out.println("[处理器] 用户断开连接: " + username);
        }
        
        // 关闭资源
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[处理器] 关闭连接时出错: " + e.getMessage());
        }
    }
    
    public String getUsername() {
        return username;
    }
    
    public boolean isConnected() {
        return connected;
    }
}
