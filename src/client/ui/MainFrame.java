package client.ui;

import client.ChatClient;
import client.MessageListener;
import common.Group;
import common.Message;
import java.awt.*;
import java.util.List;
import javax.swing.*;

/**
 * 主界面
 */
public class MainFrame extends JFrame implements MessageListener {
    private ChatClient client;
    private ChatPanel chatPanel;
    private UserListPanel userListPanel;
    
    public MainFrame() {
        super("多人聊天系统");
        client = new ChatClient();
        client.addMessageListener(this);
        
        initComponents();
        
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(1000, 700);
        setLocationRelativeTo(null);
    }
    
    private void initComponents() {
        // Setup Main UI first
        setLayout(new BorderLayout());
        
        chatPanel = new ChatPanel(client);
        userListPanel = new UserListPanel(client, this);
        
        add(userListPanel, BorderLayout.WEST);
        add(chatPanel, BorderLayout.CENTER);
        
        // Show Login Dialog
        LoginDialog loginDialog = new LoginDialog(this, client);
        loginDialog.setVisible(true);
        
        if (!loginDialog.isSucceeded()) {
            System.exit(0);
        }
        
        setTitle("多人聊天系统 - " + client.getUsername());
    }

    @Override
    public void onTextMessage(Message message) {
        SwingUtilities.invokeLater(() -> chatPanel.addMessage(message));
    }

    @Override
    public void onImageMessage(Message message) {
         SwingUtilities.invokeLater(() -> chatPanel.addMessage(message));
    }

    @Override
    public void onFileMessage(Message message) {
         SwingUtilities.invokeLater(() -> chatPanel.addMessage(message));
    }

    @Override
    public void onFileData(Message message) {
        SwingUtilities.invokeLater(() -> {
            try {
                String filename = message.getContentString("filename");
                String base64Data = message.getContentString("data");
                
                if (base64Data != null && !base64Data.isEmpty()) {
                    byte[] data = java.util.Base64.getDecoder().decode(base64Data);
                    
                    // Save to Downloads folder
                    String userHome = System.getProperty("user.home");
                    java.io.File downloadDir = new java.io.File(userHome, "Downloads/SocketChat_Downloads");
                    if (!downloadDir.exists()) {
                        downloadDir.mkdirs();
                    }
                    
                    java.io.File file = new java.io.File(downloadDir, filename);
                    
                    // Simple write (overwrite if exists or append if chunked? Assumed single chunk for now)
                    // For single chunk:
                    java.nio.file.Files.write(file.toPath(), data);
                    
                    chatPanel.addMessage(Message.createTextMessage("System", message.getSender(), Message.TargetType.USER, 
                        "文件已保存: " + file.getAbsolutePath()));
                        
                    JOptionPane.showMessageDialog(this, "收到文件: " + filename + "\n已保存至: " + file.getAbsolutePath());
                }
            } catch (Exception e) {
                e.printStackTrace();
                JOptionPane.showMessageDialog(this, "接收文件失败: " + e.getMessage());
            }
        });
    }

    @Override
    public void onUserListUpdate(List<String> users) {
        SwingUtilities.invokeLater(() -> userListPanel.updateUserList(users));
    }

    @Override
    public void onUserJoin(String username) {
        SwingUtilities.invokeLater(() -> userListPanel.addUser(username));
    }

    @Override
    public void onUserLeave(String username) {
        SwingUtilities.invokeLater(() -> userListPanel.removeUser(username));
    }

    @Override
    public void onGroupListUpdate(List<Group> groups) {
         SwingUtilities.invokeLater(() -> userListPanel.updateGroupList(groups));
    }

    @Override
    public void onGroupCreated(Group group) {
         SwingUtilities.invokeLater(() -> userListPanel.addGroup(group));
    }

    @Override
    public void onError(String error) {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, error, "错误", JOptionPane.ERROR_MESSAGE));
    }

    @Override
    public void onDisconnected() {
        SwingUtilities.invokeLater(() -> {
            JOptionPane.showMessageDialog(this, "连接已断开", "通知", JOptionPane.INFORMATION_MESSAGE);
            System.exit(0);
        });
    }

    public void onTargetSelected(String target, Message.TargetType type) {
        chatPanel.setTarget(target, type);
    }
}
