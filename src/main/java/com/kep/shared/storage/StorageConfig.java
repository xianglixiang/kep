package com.kep.shared.storage;

import io.minio.MinioClient;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@EnableConfigurationProperties(StorageConfig.StorageProperties.class)
public class StorageConfig {

    @ConfigurationProperties(prefix = "kep.storage")
    public record StorageProperties(String endpoint, String accessKey, String secretKey, String bucket) {}

    @Bean
    @Profile("!local-mock")
    static MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
            .endpoint(props.endpoint())
            .credentials(props.accessKey(), props.secretKey())
            .build();
    }
}
