package common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 群组实体类
 */
public class Group implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String groupId;         // 群组ID
    private String groupName;       // 群组名称
    private String creator;         // 创建者
    private List<String> members;   // 成员列表
    private long createTime;        // 创建时间
    
    public Group() {
        this.groupId = UUID.randomUUID().toString().substring(0, 8);
        this.members = new ArrayList<>();
        this.createTime = System.currentTimeMillis();
    }
    
    public Group(String groupName, String creator) {
        this();
        this.groupName = groupName;
        this.creator = creator;
        this.members.add(creator);
    }
    
    public Group(String groupName, String creator, List<String> members) {
        this();
        this.groupName = groupName;
        this.creator = creator;
        // 确保创建者在成员列表中
        if (!members.contains(creator)) {
            this.members.add(creator);
        }
        this.members.addAll(members);
    }
    
    public void addMember(String username) {
        if (!members.contains(username)) {
            members.add(username);
        }
    }
    
    public void removeMember(String username) {
        members.remove(username);
    }
    
    public boolean hasMember(String username) {
        return members.contains(username);
    }
    
    // Getter & Setter
    
    public String getGroupId() {
        return groupId;
    }
    
    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }
    
    public String getGroupName() {
        return groupName;
    }
    
    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }
    
    public String getCreator() {
        return creator;
    }
    
    public void setCreator(String creator) {
        this.creator = creator;
    }
    
    public List<String> getMembers() {
        return members;
    }
    
    public void setMembers(List<String> members) {
        this.members = members;
    }
    
    public long getCreateTime() {
        return createTime;
    }
    
    public void setCreateTime(long createTime) {
        this.createTime = createTime;
    }
    
    @Override
    public String toString() {
        return groupName + " (" + members.size() + "人)";
    }
}
