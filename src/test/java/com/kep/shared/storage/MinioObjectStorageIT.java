package com.kep.shared.storage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers
@Tag("integration")
class MinioObjectStorageIT {

    @Container
    static MinIOContainer minio = new MinIOContainer("minio/minio:RELEASE.2024-12-18T13-15-44Z");

    ObjectStorage storage;

    @BeforeEach
    void setUp() {
        var props = new StorageConfig.StorageProperties(
            minio.getS3URL(), minio.getUserName(), minio.getPassword(), "kep-test");
        storage = new MinioObjectStorage(StorageConfig.minioClient(props), props);
    }

    @Test
    void put_then_get_roundtrips_bytes() {
        byte[] data = "你好,知识库".getBytes(StandardCharsets.UTF_8);
        String key = "tenant-a/docs/hello.txt";

        storage.put(key, new ByteArrayInputStream(data), data.length, "text/plain");

        assertThat(new String(storage.get(key), StandardCharsets.UTF_8)).isEqualTo("你好,知识库");
    }
}
