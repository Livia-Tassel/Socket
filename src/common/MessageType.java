package common;

/**
 * 消息类型枚举
 * 定义客户端和服务器之间通信的所有消息类型
 */
public enum MessageType {
    // 连接相关
    LOGIN,              // 登录请求
    LOGIN_RESPONSE,     // 登录响应
    LOGOUT,             // 登出请求
    
    // 用户列表相关
    USER_LIST,          // 在线用户列表
    USER_JOIN,          // 用户上线通知
    USER_LEAVE,         // 用户下线通知
    
    // 消息类型
    TEXT,               // 文字消息
    IMAGE,              // 图片消息
    FILE,               // 文件传输请求
    FILE_DATA,          // 文件数据
    FILE_ACCEPT,        // 接受文件
    FILE_REJECT,        // 拒绝文件
    
    // 群组相关
    CREATE_GROUP,       // 创建群组
    GROUP_CREATED,      // 群组创建成功
    JOIN_GROUP,         // 加入群组
    LEAVE_GROUP,        // 离开群组
    GROUP_LIST,         // 群组列表
    GROUP_MEMBERS,      // 群组成员列表
    
    // 系统消息
    ERROR,              // 错误消息
    HEARTBEAT           // 心跳包
}
