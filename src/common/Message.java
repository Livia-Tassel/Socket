package common;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 消息实体类
 * 用于封装客户端和服务器之间传递的所有消息
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private MessageType type;           // 消息类型
    private String sender;              // 发送者用户名
    private String target;              // 目标（用户名或群组ID）
    private TargetType targetType;      // 目标类型
    private long timestamp;             // 时间戳
    private Map<String, Object> content; // 消息内容
    
    /**
     * 目标类型枚举
     */
    public enum TargetType {
        USER,   // 单个用户
        GROUP,  // 群组
        ALL     // 所有用户（广播）
    }
    
    public Message() {
        this.content = new HashMap<>();
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(MessageType type) {
        this();
        this.type = type;
    }
    
    public Message(MessageType type, String sender) {
        this(type);
        this.sender = sender;
    }
    
    // ==================== 静态工厂方法 ====================
    
    /**
     * 创建登录消息
     */
    public static Message createLoginMessage(String username) {
        Message msg = new Message(MessageType.LOGIN);
        msg.setSender(username);
        msg.getContent().put("username", username);
        return msg;
    }
    
    /**
     * 创建登录响应消息
     */
    public static Message createLoginResponse(boolean success, String message) {
        Message msg = new Message(MessageType.LOGIN_RESPONSE);
        msg.getContent().put("success", success);
        msg.getContent().put("message", message);
        return msg;
    }
    
    /**
     * 创建登出消息
     */
    public static Message createLogoutMessage(String username) {
        Message msg = new Message(MessageType.LOGOUT, username);
        return msg;
    }
    
    /**
     * 创建文本消息
     */
    public static Message createTextMessage(String sender, String target, 
                                            TargetType targetType, String text) {
        Message msg = new Message(MessageType.TEXT, sender);
        msg.setTarget(target);
        msg.setTargetType(targetType);
        msg.getContent().put("text", text);
        return msg;
    }
    
    /**
     * 创建图片消息
     */
    public static Message createImageMessage(String sender, String target,
                                             TargetType targetType, String filename,
                                             String base64Data, long size) {
        Message msg = new Message(MessageType.IMAGE, sender);
        msg.setTarget(target);
        msg.setTargetType(targetType);
        msg.getContent().put("filename", filename);
        msg.getContent().put("data", base64Data);
        msg.getContent().put("size", size);
        return msg;
    }
    
    /**
     * 创建文件传输请求消息
     */
    public static Message createFileMessage(String sender, String target,
                                            TargetType targetType, String filename,
                                            long size, String checksum) {
        Message msg = new Message(MessageType.FILE, sender);
        msg.setTarget(target);
        msg.setTargetType(targetType);
        msg.getContent().put("filename", filename);
        msg.getContent().put("size", size);
        msg.getContent().put("checksum", checksum);
        return msg;
    }
    
    /**
     * 创建文件数据消息
     */
    public static Message createFileDataMessage(String sender, String target,
                                                TargetType targetType, String filename,
                                                String base64Data, int chunkIndex, 
                                                int totalChunks) {
        Message msg = new Message(MessageType.FILE_DATA, sender);
        msg.setTarget(target);
        msg.setTargetType(targetType);
        msg.getContent().put("filename", filename);
        msg.getContent().put("data", base64Data);
        msg.getContent().put("chunkIndex", chunkIndex);
        msg.getContent().put("totalChunks", totalChunks);
        return msg;
    }
    
    /**
     * 创建创建群组消息
     */
    public static Message createGroupMessage(String creator, String groupName, 
                                             java.util.List<String> members) {
        Message msg = new Message(MessageType.CREATE_GROUP, creator);
        msg.getContent().put("groupName", groupName);
        msg.getContent().put("members", members);
        return msg;
    }
    
    /**
     * 创建用户列表消息
     */
    public static Message createUserListMessage(java.util.List<String> users) {
        Message msg = new Message(MessageType.USER_LIST);
        msg.getContent().put("users", users);
        return msg;
    }
    
    /**
     * 创建用户上线通知
     */
    public static Message createUserJoinMessage(String username) {
        Message msg = new Message(MessageType.USER_JOIN);
        msg.getContent().put("username", username);
        return msg;
    }
    
    /**
     * 创建用户下线通知
     */
    public static Message createUserLeaveMessage(String username) {
        Message msg = new Message(MessageType.USER_LEAVE);
        msg.getContent().put("username", username);
        return msg;
    }
    
    /**
     * 创建错误消息
     */
    public static Message createErrorMessage(String errorMsg) {
        Message msg = new Message(MessageType.ERROR);
        msg.getContent().put("error", errorMsg);
        return msg;
    }
    
    /**
     * 创建群组列表消息
     */
    public static Message createGroupListMessage(java.util.List<Group> groups) {
        Message msg = new Message(MessageType.GROUP_LIST);
        msg.getContent().put("groups", groups);
        return msg;
    }
    
    /**
     * 创建群组创建成功消息
     */
    public static Message createGroupCreatedMessage(Group group) {
        Message msg = new Message(MessageType.GROUP_CREATED);
        msg.getContent().put("group", group);
        return msg;
    }
    
    // ==================== Getter & Setter ====================
    
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public String getSender() {
        return sender;
    }
    
    public void setSender(String sender) {
        this.sender = sender;
    }
    
    public String getTarget() {
        return target;
    }
    
    public void setTarget(String target) {
        this.target = target;
    }
    
    public TargetType getTargetType() {
        return targetType;
    }
    
    public void setTargetType(TargetType targetType) {
        this.targetType = targetType;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    public Map<String, Object> getContent() {
        return content;
    }
    
    public void setContent(Map<String, Object> content) {
        this.content = content;
    }
    
    // 便捷方法获取content中的值
    public String getContentString(String key) {
        Object value = content.get(key);
        return value != null ? value.toString() : null;
    }
    
    public Boolean getContentBoolean(String key) {
        Object value = content.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        return null;
    }
    
    public Long getContentLong(String key) {
        Object value = content.get(key);
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        return null;
    }
    
    public Integer getContentInt(String key) {
        Object value = content.get(key);
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", sender='" + sender + '\'' +
                ", target='" + target + '\'' +
                ", targetType=" + targetType +
                ", timestamp=" + timestamp +
                ", content=" + content +
                '}';
    }
}
