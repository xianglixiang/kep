package com.kep.shared.storage;

import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** local-mock 下的对象存储：纯内存键值，无需 MinIO。 */
@Component
@Profile("local-mock")
public class InMemoryObjectStorage implements ObjectStorage {

    private final Map<String, byte[]> store = new ConcurrentHashMap<>();

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            store.put(key, content.readAllBytes());
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.INTERNAL, "写入内存对象失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] get(String key) {
        byte[] data = store.get(key);
        if (data == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "对象不存在: " + key);
        }
        return data;
    }

    @Override
    public void delete(String key) {
        store.remove(key);
    }
}
