package client;

import common.*;

/**
 * 消息监听器接口
 * 用于接收服务器发送的消息回调
 */
public interface MessageListener {
    
    /**
     * 收到文本消息
     */
    void onTextMessage(Message message);
    
    /**
     * 收到图片消息
     */
    void onImageMessage(Message message);
    
    /**
     * 收到文件消息
     */
    void onFileMessage(Message message);
    
    /**
     * 收到文件数据
     */
    void onFileData(Message message);
    
    /**
     * 用户列表更新
     */
    void onUserListUpdate(java.util.List<String> users);
    
    /**
     * 用户上线
     */
    void onUserJoin(String username);
    
    /**
     * 用户下线
     */
    void onUserLeave(String username);
    
    /**
     * 群组列表更新
     */
    void onGroupListUpdate(java.util.List<Group> groups);
    
    /**
     * 新群组创建
     */
    void onGroupCreated(Group group);
    
    /**
     * 收到错误消息
     */
    void onError(String errorMessage);
    
    /**
     * 连接断开
     */
    void onDisconnected();
}
