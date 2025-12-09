package client;

import common.*;
import com.google.gson.reflect.TypeToken;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * 聊天客户端
 * 负责与服务器建立连接和消息通信
 */
public class ChatClient {
    private Socket socket;
    private BufferedReader reader;
    private PrintWriter writer;
    private String username;
    private volatile boolean connected;
    private Thread receiveThread;
    private final List<MessageListener> listeners;
    
    public ChatClient() {
        this.listeners = new ArrayList<>();
        this.connected = false;
    }
    
    /**
     * 添加消息监听器
     */
    public void addMessageListener(MessageListener listener) {
        listeners.add(listener);
    }
    
    /**
     * 移除消息监听器
     */
    public void removeMessageListener(MessageListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * 连接到服务器
     */
    public boolean connect(String host, int port, String username) {
        try {
            socket = new Socket(host, port);
            reader = new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), "UTF-8"), true);
            
            this.username = username;
            this.connected = true;
            
            // 发送登录请求
            sendMessage(Message.createLoginMessage(username));
            
            // 等待登录响应
            String response = reader.readLine();
            if (response != null) {
                Message loginResponse = JsonUtils.fromJson(response);
                if (loginResponse.getType() == MessageType.LOGIN_RESPONSE) {
                    Boolean success = loginResponse.getContentBoolean("success");
                    if (success != null && success) {
                        // 登录成功，启动接收线程
                        startReceiveThread();
                        return true;
                    } else {
                        String errorMsg = loginResponse.getContentString("message");
                        System.err.println("[客户端] 登录失败: " + errorMsg);
                        disconnect();
                        return false;
                    }
                }
            }
            
            disconnect();
            return false;
            
        } catch (IOException e) {
            System.err.println("[客户端] 连接失败: " + e.getMessage());
            disconnect();
            return false;
        }
    }
    
    /**
     * 启动消息接收线程
     */
    private void startReceiveThread() {
        receiveThread = new Thread(() -> {
            try {
                String line;
                while (connected && (line = reader.readLine()) != null) {
                    try {
                        Message message = JsonUtils.fromJson(line);
                        handleMessage(message);
                    } catch (Exception e) {
                        System.err.println("[客户端] 解析消息出错: " + e.getMessage());
                    }
                }
            } catch (IOException e) {
                if (connected) {
                    System.err.println("[客户端] 接收消息时出错: " + e.getMessage());
                }
            } finally {
                if (connected) {
                    disconnect();
                    notifyDisconnected();
                }
            }
        });
        receiveThread.setDaemon(true);
        receiveThread.start();
    }
    
    /**
     * 处理接收到的消息
     */
    @SuppressWarnings("unchecked")
    private void handleMessage(Message message) {
        System.out.println("[客户端] 收到消息: " + message.getType());
        
        switch (message.getType()) {
            case TEXT:
                notifyTextMessage(message);
                break;
                
            case IMAGE:
                notifyImageMessage(message);
                break;
                
            case FILE:
                notifyFileMessage(message);
                break;
                
            case FILE_DATA:
                notifyFileData(message);
                break;
                
            case USER_LIST:
                Object usersObj = message.getContent().get("users");
                if (usersObj instanceof List) {
                    List<String> users = new ArrayList<>();
                    for (Object u : (List<?>) usersObj) {
                        users.add(u.toString());
                    }
                    notifyUserListUpdate(users);
                }
                break;
                
            case USER_JOIN:
                String joinUser = message.getContentString("username");
                if (joinUser != null) {
                    notifyUserJoin(joinUser);
                }
                break;
                
            case USER_LEAVE:
                String leaveUser = message.getContentString("username");
                if (leaveUser != null) {
                    notifyUserLeave(leaveUser);
                }
                break;
                
            case GROUP_LIST:
                // 解析群组列表
                Object groupsObj = message.getContent().get("groups");
                List<Group> groups = parseGroups(groupsObj);
                notifyGroupListUpdate(groups);
                break;
                
            case GROUP_CREATED:
                Object groupObj = message.getContent().get("group");
                Group group = parseGroup(groupObj);
                if (group != null) {
                    notifyGroupCreated(group);
                }
                break;
                
            case ERROR:
                String error = message.getContentString("error");
                if (error != null) {
                    notifyError(error);
                }
                break;
                
            default:
                System.out.println("[客户端] 未处理的消息类型: " + message.getType());
        }
    }
    
    /**
     * 解析群组列表
     */
    @SuppressWarnings("unchecked")
    private List<Group> parseGroups(Object obj) {
        List<Group> groups = new ArrayList<>();
        if (obj instanceof List) {
            for (Object item : (List<?>) obj) {
                Group group = parseGroup(item);
                if (group != null) {
                    groups.add(group);
                }
            }
        }
        return groups;
    }
    
    /**
     * 解析单个群组对象
     */
    @SuppressWarnings("unchecked")
    private Group parseGroup(Object obj) {
        if (obj instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) obj;
            Group group = new Group();
            group.setGroupId((String) map.get("groupId"));
            group.setGroupName((String) map.get("groupName"));
            group.setCreator((String) map.get("creator"));
            
            Object membersObj = map.get("members");
            if (membersObj instanceof List) {
                List<String> members = new ArrayList<>();
                for (Object m : (List<?>) membersObj) {
                    members.add(m.toString());
                }
                group.setMembers(members);
            }
            
            return group;
        }
        return null;
    }
    
    /**
     * 发送消息到服务器
     */
    public void sendMessage(Message message) {
        if (writer != null && connected) {
            String json = JsonUtils.toJson(message);
            writer.println(json);
        }
    }
    
    /**
     * 发送文本消息
     */
    public void sendTextMessage(String target, Message.TargetType targetType, String text) {
        Message msg = Message.createTextMessage(username, target, targetType, text);
        sendMessage(msg);
    }
    
    /**
     * 发送图片消息
     */
    public void sendImageMessage(String target, Message.TargetType targetType, 
                                  String filename, String base64Data, long size) {
        Message msg = Message.createImageMessage(username, target, targetType, filename, base64Data, size);
        sendMessage(msg);
    }
    
    /**
     * 发送文件消息
     */
    public void sendFileMessage(String target, Message.TargetType targetType,
                                 String filename, long size, String checksum) {
        Message msg = Message.createFileMessage(username, target, targetType, filename, size, checksum);
        sendMessage(msg);
    }
    
    /**
     * 发送文件数据
     */
    public void sendFileData(String target, Message.TargetType targetType,
                              String filename, String base64Data, int chunkIndex, int totalChunks) {
        Message msg = Message.createFileDataMessage(username, target, targetType, 
                                                    filename, base64Data, chunkIndex, totalChunks);
        sendMessage(msg);
    }
    
    /**
     * 创建群组
     */
    public void createGroup(String groupName, List<String> members) {
        Message msg = Message.createGroupMessage(username, groupName, members);
        sendMessage(msg);
    }
    
    /**
     * 断开连接
     */
    public void disconnect() {
        if (!connected) {
            return;
        }
        
        connected = false;
        
        // 发送登出消息
        if (writer != null && username != null) {
            try {
                sendMessage(Message.createLogoutMessage(username));
            } catch (Exception e) {
                // 忽略
            }
        }
        
        // 关闭资源
        try {
            if (reader != null) reader.close();
            if (writer != null) writer.close();
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException e) {
            System.err.println("[客户端] 关闭连接时出错: " + e.getMessage());
        }
        
        System.out.println("[客户端] 已断开连接");
    }
    
    /**
     * 检查是否已连接
     */
    public boolean isConnected() {
        return connected;
    }
    
    /**
     * 获取当前用户名
     */
    public String getUsername() {
        return username;
    }
    
    // ==================== 通知监听器方法 ====================
    
    private void notifyTextMessage(Message message) {
        for (MessageListener listener : listeners) {
            listener.onTextMessage(message);
        }
    }
    
    private void notifyImageMessage(Message message) {
        for (MessageListener listener : listeners) {
            listener.onImageMessage(message);
        }
    }
    
    private void notifyFileMessage(Message message) {
        for (MessageListener listener : listeners) {
            listener.onFileMessage(message);
        }
    }
    
    private void notifyFileData(Message message) {
        for (MessageListener listener : listeners) {
            listener.onFileData(message);
        }
    }
    
    private void notifyUserListUpdate(List<String> users) {
        for (MessageListener listener : listeners) {
            listener.onUserListUpdate(users);
        }
    }
    
    private void notifyUserJoin(String username) {
        for (MessageListener listener : listeners) {
            listener.onUserJoin(username);
        }
    }
    
    private void notifyUserLeave(String username) {
        for (MessageListener listener : listeners) {
            listener.onUserLeave(username);
        }
    }
    
    private void notifyGroupListUpdate(List<Group> groups) {
        for (MessageListener listener : listeners) {
            listener.onGroupListUpdate(groups);
        }
    }
    
    private void notifyGroupCreated(Group group) {
        for (MessageListener listener : listeners) {
            listener.onGroupCreated(group);
        }
    }
    
    private void notifyError(String errorMessage) {
        for (MessageListener listener : listeners) {
            listener.onError(errorMessage);
        }
    }
    
    private void notifyDisconnected() {
        for (MessageListener listener : listeners) {
            listener.onDisconnected();
        }
    }
}
