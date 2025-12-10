package client.util;

import common.Message;
import common.MessageType;
import common.JsonUtils;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.Base64;

/**
 * 历史记录管理器
 * 负责本地文件系统操作，保存和加载聊天记录及缓存文件
 */
public class HistoryManager {
    private final String username;
    private final Path baseDir;
    private final Path historyDir;
    private final Path cacheDir;

    public HistoryManager(String username) {
        this.username = username;
        // 基础目录: ChatData/{username}
        this.baseDir = Paths.get("ChatData", username);
        this.historyDir = baseDir.resolve("history");
        this.cacheDir = baseDir.resolve("cache");

        initDirectories();
    }

    private void initDirectories() {
        try {
            if (!Files.exists(baseDir)) Files.createDirectories(baseDir);
            if (!Files.exists(historyDir)) Files.createDirectories(historyDir);
            if (!Files.exists(cacheDir)) Files.createDirectories(cacheDir);
        } catch (IOException e) {
            System.err.println("[HistoryManager] 无法创建数据目录: " + e.getMessage());
        }
    }

    /**
     * 保存消息到历史记录
     * @param targetKey 会话标识 (对方用户名 或 群组ID)
     * @param message 消息对象
     */
    public synchronized void saveMessage(String targetKey, Message message) {
        Path logFile = historyDir.resolve(targetKey + ".json");
        
        // 如果是图片消息，先将图片数据另存为文件，消息中只保留路径，减小JSON体积
        Message msgToSave = processMessageForStorage(message);
        
        try (BufferedWriter writer = Files.newBufferedWriter(logFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            String json = JsonUtils.toJson(msgToSave);
            writer.write(json);
            writer.newLine();
        } catch (IOException e) {
            System.err.println("[HistoryManager] 保存消息失败: " + e.getMessage());
        }
    }

    /**
     * 加载历史记录
     * @param targetKey 会话标识
     * @return 消息列表
     */
    public synchronized List<Message> loadHistory(String targetKey) {
        List<Message> history = new ArrayList<>();
        Path logFile = historyDir.resolve(targetKey + ".json");

        if (!Files.exists(logFile)) {
            return history;
        }

        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                try {
                    Message msg = JsonUtils.fromJson(line);
                    // 如果消息引用了本地缓存图片，尝试读取以恢复 data 字段用于显示
                    restoreMessageData(msg);
                    history.add(msg);
                } catch (Exception e) {
                    System.err.println("[HistoryManager] 解析历史消息出错: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("[HistoryManager] 读取历史记录失败: " + e.getMessage());
        }
        return history;
    }

    /**
     * 处理消息以便存储 (分离大文件数据)
     */
    private Message processMessageForStorage(Message original) {
        // 如果是图片消息，且包含Base64数据，未来可在此处优化为仅存储文件路径
        // 目前为了保证兼容性，直接存储完整 Base64 数据
        
        if (original.getType() == MessageType.IMAGE) {
            String base64Data = original.getContentString("data");
            if (base64Data != null && base64Data.length() > 100) { 
                String filename = original.getContentString("filename");
                String cacheFileName = System.currentTimeMillis() + "_" + filename;
                Path cacheFile = cacheDir.resolve(cacheFileName);
                
                try {
                    // 顺便把图片存一份到 cache 目录，方便查看
                    byte[] bytes = Base64.getDecoder().decode(base64Data);
                    Files.write(cacheFile, bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return original; 
    }
    
    /**
     * 恢复消息数据 (如果有分离存储)
     */
    private void restoreMessageData(Message msg) {
        // 配合上面的 processMessageForStorage，目前我们直接存储了 Base64，不需要恢复。
        // 如果后面实现了分离存储，这里需要读取文件重填 Base64。
    }

}
