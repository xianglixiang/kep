package com.kep.shared.storage;

import com.kep.shared.error.BusinessException;
import com.kep.shared.error.ErrorCode;
import io.minio.*;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Component
@Lazy
@Profile("!local-mock")
public class MinioObjectStorage implements ObjectStorage {

    private final MinioClient client;
    private final StorageConfig.StorageProperties props;

    public MinioObjectStorage(MinioClient client, StorageConfig.StorageProperties props) {
        this.client = client;
        this.props = props;
        ensureBucket();
    }

    private void ensureBucket() {
        try {
            boolean exists = client.bucketExists(
                BucketExistsArgs.builder().bucket(props.bucket()).build());
            if (!exists) {
                client.makeBucket(MakeBucketArgs.builder().bucket(props.bucket()).build());
            }
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL, "初始化对象存储桶失败: " + e.getMessage());
        }
    }

    @Override
    public void put(String key, InputStream content, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                .bucket(props.bucket()).object(key)
                .stream(content, size, -1)
                .contentType(contentType)
                .build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL, "上传对象失败: " + e.getMessage());
        }
    }

    @Override
    public byte[] get(String key) {
        try (InputStream in = client.getObject(
                GetObjectArgs.builder().bucket(props.bucket()).object(key).build())) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "读取对象失败: " + e.getMessage());
        }
    }

    @Override
    public void delete(String key) {
        try {
            client.removeObject(RemoveObjectArgs.builder().bucket(props.bucket()).object(key).build());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL, "删除对象失败: " + e.getMessage());
        }
    }
}
