package client.ui;

import client.ChatClient;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

/**
 * 登录对话框
 */
public class LoginDialog extends JDialog {
    private JTextField hostField;
    private JTextField portField;
    private JTextField usernameField;
    private JButton loginButton;
    private JButton cancelButton;
    private ChatClient client;
    private boolean succeeded;
    
    public LoginDialog(Frame parent, ChatClient client) {
        super(parent, "登录 - 多人聊天系统", true);
        this.client = client;
        this.succeeded = false;
        
        initComponents();
        
        // 设置默认值
        hostField.setText("127.0.0.1");
        portField.setText("8888");
        
        pack();
        setResizable(false);
        setLocationRelativeTo(parent);
    }
    
    private void initComponents() {
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints cs = new GridBagConstraints();
        cs.fill = GridBagConstraints.HORIZONTAL;
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));
        
        // Host
        JLabel hostLabel = new JLabel("服务器地址:");
        cs.gridx = 0;
        cs.gridy = 0;
        cs.gridwidth = 1;
        panel.add(hostLabel, cs);
        
        hostField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 0;
        cs.gridwidth = 2;
        panel.add(hostField, cs);
        
        // Port
        JLabel portLabel = new JLabel("端口:");
        cs.gridx = 0;
        cs.gridy = 1;
        cs.gridwidth = 1;
        panel.add(portLabel, cs);
        
        portField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 1;
        cs.gridwidth = 2;
        panel.add(portField, cs);
        
        // Username
        JLabel usernameLabel = new JLabel("用户名:");
        cs.gridx = 0;
        cs.gridy = 2;
        cs.gridwidth = 1;
        panel.add(usernameLabel, cs);
        
        usernameField = new JTextField(20);
        cs.gridx = 1;
        cs.gridy = 2;
        cs.gridwidth = 2;
        panel.add(usernameField, cs);
        
        panel.setBackground(new Color(240, 240, 240));
        
        // Buttons
        loginButton = new JButton("登录");
        loginButton.addActionListener(e -> attemptLogin());
        
        cancelButton = new JButton("取消");
        cancelButton.addActionListener(e -> dispose());
        
        JPanel bp = new JPanel();
        bp.add(loginButton);
        bp.add(cancelButton);
        bp.setBackground(new Color(240, 240, 240));
        
        getContentPane().add(panel, BorderLayout.CENTER);
        getContentPane().add(bp, BorderLayout.PAGE_END);
    }
    
    private void attemptLogin() {
        String host = hostField.getText().trim();
        String portStr = portField.getText().trim();
        String username = usernameField.getText().trim();
        
        if (host.isEmpty() || portStr.isEmpty() || username.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "请填写所有字段",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this,
                    "端口必须是数字",
                    "错误",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        loginButton.setEnabled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
        
        // 在后台线程连接
        new SwingWorker<Boolean, Void>() {
            @Override
            protected Boolean doInBackground() throws Exception {
                return client.connect(host, port, username);
            }
            
            @Override
            protected void done() {
                loginButton.setEnabled(true);
                setCursor(Cursor.getDefaultCursor());
                
                try {
                    if (get()) {
                        succeeded = true;
                        dispose();
                    } else {
                        JOptionPane.showMessageDialog(LoginDialog.this,
                                "连接失败，请检查服务器地址或用户名是否重复",
                                "错误",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }.execute();
    }
    
    public boolean isSucceeded() {
        return succeeded;
    }
}
