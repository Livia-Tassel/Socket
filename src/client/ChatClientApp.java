package client;

import client.ui.MainFrame;
import com.formdev.flatlaf.intellijthemes.FlatOneDarkIJTheme;
import javax.swing.*;

/**
 * 聊天客户端应用程序入口
 */
public class ChatClientApp {
    
    public static void main(String[] args) {
        // 设置现代化的UI外观
        try {
            FlatOneDarkIJTheme.setup();
            UIManager.put("Button.arc", 10);
            UIManager.put("Component.arc", 10);
            UIManager.put("TextComponent.arc", 10);
            UIManager.put("ScrollBar.width", 10);
            UIManager.put("ScrollBar.thumbArc", 999);
            UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
        } catch (Exception e) {
            System.err.println("无法设置UI外观: " + e.getMessage());
        }
        
        // 在EDT线程中启动GUI
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });
    }
}
