package server;

import common.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 客户端管理器
 * 管理所有在线客户端连接
 */
public class ClientManager {
    // 用户名到ClientHandler的映射
    private final Map<String, ClientHandler> clients;
    
    public ClientManager() {
        this.clients = new ConcurrentHashMap<>();
    }
    
    /**
     * 添加客户端
     */
    public void addClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("[客户端管理器] 添加客户端: " + username + ", 当前在线: " + clients.size());
    }
    
    /**
     * 移除客户端
     */
    public void removeClient(String username) {
        clients.remove(username);
        System.out.println("[客户端管理器] 移除客户端: " + username + ", 当前在线: " + clients.size());
    }
    
    /**
     * 获取客户端处理器
     */
    public ClientHandler getClient(String username) {
        return clients.get(username);
    }
    
    /**
     * 检查用户是否在线
     */
    public boolean isUserOnline(String username) {
        return clients.containsKey(username);
    }
    
    /**
     * 获取所有在线用户名列表
     */
    public List<String> getOnlineUsernames() {
        return new ArrayList<>(clients.keySet());
    }
    
    /**
     * 获取在线用户数量
     */
    public int getOnlineCount() {
        return clients.size();
    }
    
    /**
     * 向指定用户发送消息
     */
    public boolean sendToUser(String username, Message message) {
        ClientHandler handler = clients.get(username);
        if (handler != null && handler.isConnected()) {
            handler.sendMessage(message);
            return true;
        }
        return false;
    }
    
    /**
     * 向多个用户发送消息
     */
    public void sendToUsers(List<String> usernames, Message message) {
        for (String username : usernames) {
            sendToUser(username, message);
        }
    }
    
    /**
     * 广播消息给所有在线用户
     */
    public void broadcast(Message message) {
        for (ClientHandler handler : clients.values()) {
            if (handler.isConnected()) {
                handler.sendMessage(message);
            }
        }
    }
    
    /**
     * 广播消息给除指定用户外的所有在线用户
     */
    public void broadcastExcept(Message message, String exceptUsername) {
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            if (!entry.getKey().equals(exceptUsername) && entry.getValue().isConnected()) {
                entry.getValue().sendMessage(message);
            }
        }
    }
    
    /**
     * 断开所有客户端连接
     */
    public void disconnectAll() {
        for (ClientHandler handler : clients.values()) {
            handler.disconnect();
        }
        clients.clear();
    }
}
