package common;

import java.io.Serializable;

/**
 * 用户实体类
 */
public class User implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String username;        // 用户名
    private boolean online;         // 是否在线
    private long loginTime;         // 登录时间
    
    public User() {
    }
    
    public User(String username) {
        this.username = username;
        this.online = true;
        this.loginTime = System.currentTimeMillis();
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public boolean isOnline() {
        return online;
    }
    
    public void setOnline(boolean online) {
        this.online = online;
    }
    
    public long getLoginTime() {
        return loginTime;
    }
    
    public void setLoginTime(long loginTime) {
        this.loginTime = loginTime;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return username != null ? username.equals(user.username) : user.username == null;
    }
    
    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }
    
    @Override
    public String toString() {
        return username;
    }
}
