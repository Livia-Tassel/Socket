package server;

import common.*;
import java.util.List;

/**
 * 消息路由器
 * 负责根据消息目标类型将消息路由到正确的接收者
 */
public class MessageRouter {
    private final ClientManager clientManager;
    private final GroupManager groupManager;
    
    public MessageRouter(ClientManager clientManager, GroupManager groupManager) {
        this.clientManager = clientManager;
        this.groupManager = groupManager;
    }
    
    /**
     * 路由消息到目标
     */
    public void routeMessage(Message message) {
        if (message.getTargetType() == null) {
            System.err.println("[消息路由] 消息缺少目标类型");
            return;
        }
        
        switch (message.getTargetType()) {
            case USER:
                routeToUser(message);
                break;
            case GROUP:
                routeToGroup(message);
                break;
            case ALL:
                routeToAll(message);
                break;
            default:
                System.err.println("[消息路由] 未知的目标类型: " + message.getTargetType());
        }
    }
    
    /**
     * 路由消息到单个用户（一对一通信）
     */
    private void routeToUser(Message message) {
        String target = message.getTarget();
        if (target == null || target.isEmpty()) {
            sendErrorToSender(message.getSender(), "目标用户不能为空");
            return;
        }
        
        // 发送给目标用户
        boolean sent = clientManager.sendToUser(target, message);
        if (!sent) {
            sendErrorToSender(message.getSender(), "用户 " + target + " 不在线");
        }
        
        // 同时也发送给发送者自己（用于显示在聊天窗口）
        // 注：发送者的本地UI通常会直接显示自己发送的消息，这里可选
        
        System.out.println("[消息路由] 消息从 " + message.getSender() + " 发送到 " + target);
    }
    
    /**
     * 路由消息到群组（群组通信）
     */
    private void routeToGroup(Message message) {
        String groupId = message.getTarget();
        if (groupId == null || groupId.isEmpty()) {
            sendErrorToSender(message.getSender(), "群组ID不能为空");
            return;
        }
        
        // 检查发送者是否是群组成员
        if (!groupManager.isGroupMember(groupId, message.getSender())) {
            sendErrorToSender(message.getSender(), "您不是该群组的成员");
            return;
        }
        
        // 获取群组成员并发送消息
        List<String> members = groupManager.getGroupMembers(groupId);
        for (String member : members) {
            // 不发送给发送者自己（避免重复显示）
            // 如果需要发送者也收到，可以去掉这个判断
            if (!member.equals(message.getSender())) {
                clientManager.sendToUser(member, message);
            }
        }
        
        System.out.println("[消息路由] 群组消息从 " + message.getSender() + 
                          " 发送到群组 " + groupId + " (" + members.size() + " 成员)");
    }
    
    /**
     * 广播消息给所有用户
     */
    private void routeToAll(Message message) {
        clientManager.broadcastExcept(message, message.getSender());
        System.out.println("[消息路由] 广播消息从 " + message.getSender());
    }
    
    /**
     * 向发送者发送错误消息
     */
    private void sendErrorToSender(String sender, String errorMsg) {
        Message error = Message.createErrorMessage(errorMsg);
        clientManager.sendToUser(sender, error);
    }
}
