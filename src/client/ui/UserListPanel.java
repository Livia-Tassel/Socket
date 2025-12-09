package client.ui;

import client.ChatClient;
import common.Group;
import common.Message;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.ArrayList;
import javax.swing.*;

/**
 * 用户列表面板
 * 显示在线用户和群组，支持切换
 */
public class UserListPanel extends JPanel {
    private ChatClient client;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JList<Group> groupList;
    private DefaultListModel<Group> groupListModel;
    private JTabbedPane tabbedPane;
    private MainFrame mainFrame;
    
    public UserListPanel(ChatClient client, MainFrame mainFrame) {
        this.client = client;
        this.mainFrame = mainFrame;
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(250, 0));
        
        initComponents();
    }
    
    private void initComponents() {
        // Users List
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        userList.setCellRenderer(new DefaultListCellRenderer() {
             @Override
             public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                 super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                 setIcon(UIManager.getIcon("FileView.fileIcon")); // Placeholder icon
                 setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                 return this;
             }
        });
        
        userList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    String selectedUser = userList.getSelectedValue();
                    if (selectedUser != null) {
                        mainFrame.onTargetSelected(selectedUser, Message.TargetType.USER);
                        groupList.clearSelection();
                    }
                }
            }
        });
        
        // Groups List
        groupListModel = new DefaultListModel<>();
        groupList = new JList<>(groupListModel);
        groupList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        groupList.setCellRenderer(new DefaultListCellRenderer() {
             @Override
             public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                 super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                 if (value instanceof Group) {
                     setText(((Group) value).getGroupName());
                 }
                 setIcon(UIManager.getIcon("FileView.directoryIcon")); // Placeholder icon
                 setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
                 return this;
             }
        });
        
        groupList.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent evt) {
                if (evt.getClickCount() == 1) {
                    Group selectedGroup = groupList.getSelectedValue();
                    if (selectedGroup != null) {
                        // Use Group Name or ID? Protocol uses ID usually or Name if unique. 
                        // Message.java setTarget uses String. 
                        // Let's assume GroupName is unique or we store ID.
                        // Message needs target string.
                        mainFrame.onTargetSelected(selectedGroup.getGroupId(), Message.TargetType.GROUP);
                        userList.clearSelection();
                    }
                }
            }
        });

        // Tabs
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("在线用户", new JScrollPane(userList));
        tabbedPane.addTab("我的群组", new JScrollPane(groupList));
        
        add(tabbedPane, BorderLayout.CENTER);
        
        // Bottom Panel (Buttons)
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        JButton createGroupBtn = new JButton("建群");
        createGroupBtn.addActionListener(e -> createGroup());
        JButton refreshBtn = new JButton("刷新"); 
        // Refresh depends on server pushing updates, but maybe we can request? 
        // Protocol doesn't explicitly have "Request User List", but User List is push.
        // We can ignore refresh for now or implement if protocol supports calls.
        
        bottomPanel.add(createGroupBtn);
        // bottomPanel.add(refreshBtn);
        
        add(bottomPanel, BorderLayout.PAGE_END);
    }
    
    public void updateUserList(List<String> users) {
        userListModel.clear();
        for (String user : users) {
            if (!user.equals(client.getUsername())) { // Don't show self
                userListModel.addElement(user);
            }
        }
    }
    
    public void addUser(String username) {
        if (!username.equals(client.getUsername()) && !userListModel.contains(username)) {
            userListModel.addElement(username);
        }
    }
    
    public void removeUser(String username) {
        userListModel.removeElement(username);
    }
    
    public void updateGroupList(List<Group> groups) {
        groupListModel.clear();
        for (Group group : groups) {
            groupListModel.addElement(group);
        }
    }
    
    public void addGroup(Group group) {
        groupListModel.addElement(group);
    }
    
    private void createGroup() {
        // Simple dialog to enter group name and select members
        // For now just name
        String groupName = JOptionPane.showInputDialog(this, "请输入群组名称:");
        if (groupName != null && !groupName.trim().isEmpty()) {
            // Need to select members. For simplicity, just create empty group or all?
            // "Select members" dialog is better.
            
            // Get all online users
            List<String> allUsers = new ArrayList<>();
            for(int i=0; i<userListModel.getSize(); i++) {
                allUsers.add(userListModel.getElementAt(i));
            }
            
            if (allUsers.isEmpty()) {
                 client.createGroup(groupName, new ArrayList<>()); // Empty group or self?
                 return;
            }

            JList<String> list = new JList<>(allUsers.toArray(new String[0]));
            JOptionPane.showMessageDialog(
              this, list, "选择群成员 (按住Ctrl多选)", JOptionPane.PLAIN_MESSAGE);
             
            List<String> selected = list.getSelectedValuesList();
            selected.add(client.getUsername()); // Add self
            
            client.createGroup(groupName, selected);
        }
    }
}
