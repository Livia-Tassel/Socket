package server;

import common.Group;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 群组管理器
 * 管理所有群组信息
 */
public class GroupManager {
    // 群组ID到Group的映射
    private final Map<String, Group> groups;
    
    public GroupManager() {
        this.groups = new ConcurrentHashMap<>();
    }
    
    /**
     * 创建群组
     */
    public Group createGroup(String groupName, String creator, List<String> members) {
        Group group = new Group(groupName, creator, members);
        groups.put(group.getGroupId(), group);
        System.out.println("[群组管理器] 创建群组: " + groupName + " (ID: " + group.getGroupId() + ")");
        return group;
    }
    
    /**
     * 获取群组
     */
    public Group getGroup(String groupId) {
        return groups.get(groupId);
    }
    
    /**
     * 删除群组
     */
    public void deleteGroup(String groupId) {
        Group removed = groups.remove(groupId);
        if (removed != null) {
            System.out.println("[群组管理器] 删除群组: " + removed.getGroupName());
        }
    }
    
    /**
     * 添加成员到群组
     */
    public boolean addMemberToGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.addMember(username);
            return true;
        }
        return false;
    }
    
    /**
     * 从群组移除成员
     */
    public boolean removeMemberFromGroup(String groupId, String username) {
        Group group = groups.get(groupId);
        if (group != null) {
            group.removeMember(username);
            // 如果群组没有成员了，删除群组
            if (group.getMembers().isEmpty()) {
                deleteGroup(groupId);
            }
            return true;
        }
        return false;
    }
    
    /**
     * 获取群组成员列表
     */
    public List<String> getGroupMembers(String groupId) {
        Group group = groups.get(groupId);
        if (group != null) {
            return new ArrayList<>(group.getMembers());
        }
        return Collections.emptyList();
    }
    
    /**
     * 检查用户是否是群组成员
     */
    public boolean isGroupMember(String groupId, String username) {
        Group group = groups.get(groupId);
        return group != null && group.hasMember(username);
    }
    
    /**
     * 获取用户所在的所有群组
     */
    public List<Group> getGroupsForUser(String username) {
        return groups.values().stream()
                .filter(g -> g.hasMember(username))
                .collect(Collectors.toList());
    }
    
    /**
     * 获取所有群组
     */
    public List<Group> getAllGroups() {
        return new ArrayList<>(groups.values());
    }
}
