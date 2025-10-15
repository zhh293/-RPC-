package cn.itcast.RPC框架;

public interface Serialize {
    byte[] serialize(Object obj);
    <T> T deserialize(byte[] bytes, Class<T> clazz);
}
