# 多人聊天系统 (Multi-Person Chat System)

这是一个基于 Java Socket (TCP) 开发的多人在线聊天系统，采用了 C/S（客户端/服务器）架构。系统支持多用户实时在线、一对一私聊、群组聊天、以及图片和文件的实时传输。界面采用 Swing + FlatLaf 实现现代化 UI。

## 1. 系统架构与代码结构

### 1.1 系统架构
系统采用经典的 **Client-Server** 架构：
- **Server (服务器端)**: 负责监听端口，维护所有客户端的长连接，管理在线用户列表和群组信息，并充当消息路由中枢，将消息转发给目标用户或群组。
- **Client (客户端)**: 提供图形化用户界面 (GUI)，与服务器建立 TCP 连接。负责封装用户输入的数据为协议消息发送给服务器，并解析服务器推送的消息进行展示。

### 1.2 代码结构
项目源代码位于 `src` 目录下，包结构如下：

*   **`common` (公共模块)**
    *   `Message.java`: 核心消息实体类，定义了通信协议的数据包结构。
    *   `MessageType.java`: 枚举类，定义所有支持的消息类型（如 LOGIN, TEXT, IMAGE, FILE 等）。
    *   `User.java` / `Group.java`: 用户和群组的实体模型。
    *   `JsonUtils.java`: 基于 Gson 的 JSON 序列化/反序列化工具。
*   **`server` (服务器端)**
    *   `ChatServer.java`: 程序入口，负责启动 ServerSocket 和线程池。
    *   `ClientManager.java`: 管理所有 `ClientHandler` 和在线用户列表。
    *   `GroupManager.java`: 管理群组生命周期和成员关系。
    *   `ClientHandler.java`: 具体的客户端连接处理线程，负责读取 socket 输入流。
    *   `MessageRouter.java`: 消息路由逻辑，决定消息发给谁。
*   **`client` (客户端)**
    *   `ChatClientApp.java`: 客户端程序入口。
    *   `ChatClient.java`: 负责 Socket 连接维护、消息发送和异步接收。
    *   `ui` 包:
        *   `MainFrame.java`: 主窗口框架。
        *   `LoginDialog.java`: 登录对话框。
        *   `UserListPanel.java`: 在线用户和群组列表面板。
        *   `ChatPanel.java`: 聊天核心面板（消息展示、输入、文件发送）。

---

## 2. 详细应用协议设计 (Application Protocol Design)

本系统设计了一套应用层通信协议，运行在 TCP 传输层之上。

### 2.1 设计理念
1.  **基于文本的一致性**: 采用 **JSON** 作为数据交换格式。相比二进制协议，JSON 具有良好的可读性和调试性，且易于扩展（添加新字段不影响旧解析）。
2.  **消息驱动**: 所有的交互（登录、聊天、系统通知）都被抽象为 "Message"，通过 `type` 字段区分行为。
3.  **定界符**: 使用标准换行符 `\n` 作为 TCP 字节流中消息的定界符，确保能够从流中正确分割出完整的 JSON 对象。

### 2.2 协议数据结构
所有消息均序列化为如下 JSON 结构：

```json
{
  "type": "MESSAGE_TYPE",         // 消息类型 (枚举字符串)
  "sender": "sender_username",    // 发送者用户名
  "target": "target_id",          // 目标 (用户名 或 群组ID)
  "targetType": "USER",           // 目标类型: "USER" (私聊) 或 "GROUP" (群聊)
  "timestamp": 1702123456789,     // 时间戳
  "content": {                    // 消息内容载荷 (Map<String, Object>)
    "text": "Hello",              // (示例) 文本内容
    "data": "base64...",          // (示例) 文件/图片数据
    "filename": "doc.pdf"         // (示例) 文件名
  }
}
```

### 2.3 关键消息流程定义

#### 2.3.1 用户登录
*   **Request**: `Client` -> `Server`
    *   `type`: **LOGIN**
    *   `content`: `{ "username": "Alice" }`
*   **Response**: `Server` -> `Client`
    *   `type`: **LOGIN_RESPONSE**
    *   `content`: `{ "success": true, "message": "Login successful" }`
*   **Broadcast**: `Server` -> `All Clients`
    *   `type`: **USER_JOIN**, `content`: `{ "username": "Alice" }`
*   **Sync**: `Server` -> `Client`
    *   `type`: **USER_LIST**, `content`: `{ "users": ["Alice", "Bob", ...] }`

#### 2.3.2 聊天消息 (文本/图片/文件)
发送者 (Alice) 发送给 接收者 (Bob) 或 群组 (GroupA)：

*   **Message**: `Client A` -> `Server` -> `Client B` (or `Group Members`)
    *   `type`: **TEXT** / **IMAGE** / **FILE** / **FILE_DATA**
    *   `targetType`: "USER" 或 "GROUP"
    *   **IMAGE/FILE**: 使用 Base64 编码文件内容放入 `content.data` 字段。
    *   **FILE**: 仅包含元数据（文件名、大小），用于通知接收方。
    *   **FILE_DATA**: 包含实际文件内容数据的片段。

#### 2.3.3 群组管理
*   **Create**: `Client` -> `Server` (`CREATE_GROUP`) -> `Members` (`GROUP_CREATED`)
    *   创建者指定群名和成员列表，服务器分配唯一 GroupID 并通知所有成员。

---

## 3. 环境准备 (Prerequisites)

### 3.1 开发工具
*   **JDK**: 版本 11 或更高 (必须)。您可以运行 `java -version` 检查。
*   **OS**: Windows, macOS, 或 Linux。

### 3.2 依赖库
本项目依赖以下第三方库（已包含在项目 `target/lib` 目录中，无需额外下载）：
1.  **Gson** (2.10.1): 用于 JSON 解析。
2.  **FlatLaf** (3.2.5): 用于美化 Swing 界面。

---

## 4. 编译与运行 (Build & Run)

您可以使用以下命令在命令行中手动编译和运行项目。

### 4.1 编译项目
请在项目根目录 (`Socket/`) 终端下执行：

```bash
# 1. 创建编译输出目录
mkdir -p target/classes

# 2. 编译所有源文件
javac -d target/classes \
-cp "target/lib/*" \
src/common/*.java \
src/server/*.java \
src/client/*.java \
src/client/ui/*.java
```

### 4.2 运行服务器
```bash
java -cp "target/classes:target/lib/*" server.ChatServer
```
*成功标志*: 控制台输出 "端口: 8888, 等待客户端连接..."

### 4.3 运行客户端
可以打开多个终端窗口来启动多个客户端实例：
```bash
java -cp "target/classes:target/lib/*" client.ChatClientApp
```

---

## 5. 测试效果与结论

### 5.1 测试场景验证
经过详细测试，系统各功能表现如下：

1.  **用户登录与列表同步**:
    *   ✅ 用户登录后，其名称立即出现在所有其他在线用户的左侧列表中。
    *   ✅ 用户退出后，列表中自动移除该用户。

2.  **一对一聊天**:
    *   ✅ 用户 A 点击列表中的 用户 B，可以发送文本消息。
    *   ✅ B 收到消息高亮提示，点击 A 后可查看完整对话历史。

3.  **群组功能**:
    *   ✅ 用户可创建群组（如 "Project Team"），选择多个成员。
    *   ✅ 所有被选中的成员会立即收到群组通知，群组出现在左侧 "我的群组" 列表中。
    *   ✅ 群内消息实现广播，任一成员发送，其他人均可收到。

4.  **富媒体传输**:
    *   ✅ **图片传输**: 选择 PNG/JPG 图片发送，接收方聊天窗口直接渲染显示图片。
    *   ✅ **文件传输**: 发送任意文件，接收方自动接收并保存至 `~/Downloads/SocketChat_Downloads/` 目录，且有弹窗提示。

### 5.2 结论
本项目成功实现了一个功能完备的多人聊天系统。通过自定义的应用层 JSON 协议，不仅解决了文本通信问题，还高效处理了二进制数据（图片/文件）的传输。系统稳定性良好，能够正确处理并发连接和动态的用户上下线事件，达到了实验预期的所有技术指标。
