package cn.itcast.RPC框架;

import cn.hutool.json.JSONUtil;

public class JsonSerializer implements Serialize{

    @Override
    public byte[] serialize(Object obj) {
        String jsonStr = JSONUtil.toJsonStr(obj);
        return jsonStr.getBytes();
    }

    @Override
    public <T> T deserialize(byte[] data, Class<T> clazz) {
        String jsonStr = new String(data);
        return JSONUtil.toBean(jsonStr, clazz);
    }
}
