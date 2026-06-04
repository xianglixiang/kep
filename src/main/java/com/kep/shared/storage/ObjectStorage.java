package com.kep.shared.storage;

import java.io.InputStream;

/** 原始文件（Word/Excel/PPT 等）存储端口。键约定以 tenant_id 为前缀实现租户隔离。 */
public interface ObjectStorage {

    void put(String key, InputStream content, long size, String contentType);

    byte[] get(String key);

    void delete(String key);
}
