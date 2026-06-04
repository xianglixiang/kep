package com.kep.shared.storage;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryObjectStorageTest {

    private final ObjectStorage storage = new InMemoryObjectStorage();

    @Test
    void put_then_get_roundtrips_bytes() {
        byte[] data = "你好,知识库".getBytes(StandardCharsets.UTF_8);
        String key = "tenant-a/docs/hello.txt";

        storage.put(key, new ByteArrayInputStream(data), data.length, "text/plain");

        assertThat(new String(storage.get(key), StandardCharsets.UTF_8)).isEqualTo("你好,知识库");
    }
}
