package common;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * JSON工具类
 * 使用Gson进行消息的序列化和反序列化
 */
public class JsonUtils {
    private static final Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .create();
    
    private static final Gson compactGson = new Gson();
    
    /**
     * 将对象转换为JSON字符串（紧凑格式，用于网络传输）
     */
    public static String toJson(Object obj) {
        return compactGson.toJson(obj);
    }
    
    /**
     * 将对象转换为JSON字符串（美化格式，用于调试）
     */
    public static String toPrettyJson(Object obj) {
        return gson.toJson(obj);
    }
    
    /**
     * 将JSON字符串转换为Message对象
     */
    public static Message fromJson(String json) {
        return compactGson.fromJson(json, Message.class);
    }
    
    /**
     * 将JSON字符串转换为指定类型的对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        return compactGson.fromJson(json, clazz);
    }
    
    /**
     * 获取Gson实例（用于更复杂的操作）
     */
    public static Gson getGson() {
        return compactGson;
    }
}
