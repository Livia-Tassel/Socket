package client.ui;

import client.ChatClient;
import common.Message;
import common.MessageType;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.nio.file.Files;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.*;

/**
 * 聊天面板
 * 显示消息历史和输入区域
 */
public class ChatPanel extends JPanel {
    private ChatClient client;
    private JTextPane messagePane;
    private StyledDocument doc;
    private JTextArea inputArea;
    private JButton sendButton;
    private JButton imageButton;
    private JButton fileButton;
    private JLabel targetLabel;
    
    private String currentTarget;
    private Message.TargetType currentTargetType;
    
    // 简单的内存消息历史存储: targetId -> List<Message>
    private Map<String, List<Message>> chatHistory;

    public ChatPanel(ChatClient client) {
        this.client = client;
        this.chatHistory = new HashMap<>();
        setLayout(new BorderLayout());
        
        initComponents();
    }
    
    private void initComponents() {
        // 顶部: 目标名称
        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.setBorder(new EmptyBorder(5, 10, 5, 10));
        topPanel.setBackground(new Color(230, 230, 230));
        targetLabel = new JLabel("未选择聊天对象");
        targetLabel.setFont(new Font("SansSerif", Font.BOLD, 14));
        topPanel.add(targetLabel, BorderLayout.CENTER);
        add(topPanel, BorderLayout.NORTH);
        
        // 中间: 消息显示区域
        messagePane = new JTextPane();
        messagePane.setEditable(false);
        doc = messagePane.getStyledDocument();
        JScrollPane scrollPane = new JScrollPane(messagePane);
        add(scrollPane, BorderLayout.CENTER);
        
        // 底部: 输入和按钮
        JPanel bottomPanel = new JPanel(new BorderLayout());
        
        // 工具栏 (图片、文件)
        JPanel toolBar = new JPanel(new FlowLayout(FlowLayout.LEFT));
        imageButton = new JButton("图片");
        fileButton = new JButton("文件");
        toolBar.add(imageButton);
        toolBar.add(fileButton);
        bottomPanel.add(toolBar, BorderLayout.NORTH);
        
        // 输入框
        inputArea = new JTextArea(3, 20);
        inputArea.setLineWrap(true);
        JScrollPane inputScroll = new JScrollPane(inputArea);
        bottomPanel.add(inputScroll, BorderLayout.CENTER);
        
        // 发送按钮
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sendButton = new JButton("发送");
        btnPanel.add(sendButton);
        bottomPanel.add(btnPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
        
        // 事件监听
        sendButton.addActionListener(e -> sendMessage());
        imageButton.addActionListener(e -> sendImage());
        fileButton.addActionListener(e -> sendFile());
        
        // Ctrl+Enter 发送
        inputArea.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.isControlDown() && e.getKeyCode() == KeyEvent.VK_ENTER) {
                    sendMessage();
                }
            }
        });
    }
    
    public void setTarget(String target, Message.TargetType type) {
        this.currentTarget = target;
        this.currentTargetType = type;
        
        if (type == Message.TargetType.GROUP) {
             targetLabel.setText("群组: " + target);
        } else {
             targetLabel.setText("用户: " + target);
        }
        
        refreshMessageDisplay();
    }
    
    private void refreshMessageDisplay() {
        messagePane.setText("");
        if (currentTarget == null) return;
        
        List<Message> history = chatHistory.getOrDefault(currentTarget, new ArrayList<>());
        for (Message msg : history) {
            appendMessageToPane(msg);
        }
    }
    
    public void addMessage(Message msg) {
        
        // 判断消息属于哪个会话
        String key = null;
        if (msg.getType() == MessageType.TEXT || 
            msg.getType() == MessageType.IMAGE || 
            msg.getType() == MessageType.FILE) {
            
            if (msg.getTargetType() == Message.TargetType.GROUP) {
                key = msg.getTarget(); // 群聊消息归属于群组ID
            } else {
                // 私聊
                if (msg.getSender().equals(client.getUsername())) {
                    key = msg.getTarget(); // 我发给别人的，归属于目标
                } else {
                    key = msg.getSender(); // 别人发给我的，归属于发送者
                }
            }
        }
        
        if (key != null) {
            chatHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(msg);
            
            // 如果当前正在看这个会话，则显示
            if (key.equals(currentTarget)) {
                appendMessageToPane(msg);
            }
        }
    }
    
    private void appendMessageToPane(Message msg) {
        try {
            boolean isSelf = msg.getSender().equals(client.getUsername());
            String title = msg.getSender() + " (" + new java.util.Date(msg.getTimestamp()).toString() + "):\n";
            
            SimpleAttributeSet nameAttrs = new SimpleAttributeSet();
            StyleConstants.setBold(nameAttrs, true);
            StyleConstants.setForeground(nameAttrs, isSelf ? Color.BLUE : Color.GREEN.darker());
            
            doc.insertString(doc.getLength(), title, nameAttrs);
            
            if (msg.getType() == MessageType.TEXT) {
                String text = msg.getContentString("text");
                doc.insertString(doc.getLength(), text + "\n", null);
            } else if (msg.getType() == MessageType.IMAGE) {
                doc.insertString(doc.getLength(), "[图片] " + msg.getContentString("filename") + "\n", null);
                
                String base64Data = msg.getContentString("data");
                if (base64Data != null && !base64Data.isEmpty()) {
                    try {
                        byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                        ImageIcon icon = new ImageIcon(imageBytes);
                        
                        // Scale image if too large
                        int maxWidth = 200;
                        if (icon.getIconWidth() > maxWidth) {
                            int newHeight = (int) ((double) icon.getIconHeight() * maxWidth / icon.getIconWidth());
                            Image img = icon.getImage().getScaledInstance(maxWidth, newHeight, Image.SCALE_SMOOTH);
                            icon = new ImageIcon(img);
                        }
                        
                        messagePane.setCaretPosition(doc.getLength());
                        messagePane.insertIcon(icon);
                        doc.insertString(doc.getLength(), "\n", null);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Base64 decode error: " + e.getMessage());
                    }
                }
            } else if (msg.getType() == MessageType.FILE) {
                doc.insertString(doc.getLength(), "[文件] " + msg.getContentString("filename") + " (尺寸: " + msg.getContentLong("size") + " bytes)\n", null);
            }
            
            doc.insertString(doc.getLength(), "\n", null);
            messagePane.setCaretPosition(doc.getLength());
            
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }
    
    private void sendMessage() {
        if (currentTarget == null) {
            JOptionPane.showMessageDialog(this, "请选择聊天对象");
            return;
        }
        
        String text = inputArea.getText().trim();
        if (text.isEmpty()) return;
        
        client.sendTextMessage(currentTarget, currentTargetType, text);
        inputArea.setText("");
        
        // 客户端收到服务器转发回来的消息再显示，或者自己先显示？
        // 通常socket聊天自己发的直接显示，但为了保证顺序和确认，收到服务器回执最好。
        // 这里简化：假设服务器会转发给自己（对于群聊）或自己手动添加。
        // ChatClient好像没有把发的存历史逻辑，所以这里最好自己addMessage
        // 但如果服务器回传，会有双重消息。检查Server逻辑：
        // MessageRouter: handleTextMessage forwards to target. If target is USER, sends to target. Does NOT send back to sender?
        // Server usually doesn't echo back to sender for 1-to-1.
        // So we should add to history locally.
        
        // Create a local message object to display immediately
        // Note: Timestamp might be slightly diff from server but ok for display
        Message localMsg = Message.createTextMessage(client.getUsername(), currentTarget, currentTargetType, text);
        addMessage(localMsg);
    }
    
    private void sendImage() {
        if (currentTarget == null) return;
        
        JFileChooser chooser = new JFileChooser();
        chooser.setFileFilter(new FileNameExtensionFilter("Images", "jpg", "png", "gif"));
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64 = Base64.getEncoder().encodeToString(bytes);
                client.sendImageMessage(currentTarget, currentTargetType, file.getName(), base64, file.length());
                
                Message localMsg = Message.createImageMessage(client.getUsername(), currentTarget, currentTargetType, file.getName(), base64, file.length());
                addMessage(localMsg);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "发送图片失败: " + e.getMessage());
            }
        }
    }
    
    private void sendFile() {
        if (currentTarget == null) return;
        
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            try {
                // 1. 发送文件请求 (Header)
                client.sendFileMessage(currentTarget, currentTargetType, file.getName(), file.length(), "chk");
                
                // 2. 发送文件数据 (Body) - 这里简化为一次性发送，大文件需分片
                byte[] bytes = Files.readAllBytes(file.toPath());
                String base64Data = Base64.getEncoder().encodeToString(bytes);
                
                // 发送数据包 (chunkIndex=0, totalChunks=1)
                client.sendFileData(currentTarget, currentTargetType, file.getName(), base64Data, 0, 1);
                
                JOptionPane.showMessageDialog(this, "文件已发送: " + file.getName());
                Message localMsg = Message.createFileMessage(client.getUsername(), currentTarget, currentTargetType, file.getName(), file.length(), "chk");
                addMessage(localMsg);
                
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "发送文件失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
