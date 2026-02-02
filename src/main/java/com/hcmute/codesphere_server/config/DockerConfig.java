package com.hcmute.codesphere_server.config;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.core.DefaultDockerClientConfig;
import com.github.dockerjava.core.DockerClientImpl;
import com.github.dockerjava.zerodep.ZerodepDockerHttpClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;
import java.time.Duration;

/**
 * Configuration cho Docker Client
 * Sử dụng Docker Java API để tương tác với Docker Engine
 * Dùng zerodep transport để hỗ trợ cả Windows (named pipe) và Linux/Mac (Unix socket)
 */
@Slf4j
@Configuration
public class DockerConfig {

    // Docker host - Windows: npipe:////./pipe/docker_engine, Linux/Mac: unix:///var/run/docker.sock
    @Value("${docker.host:npipe:////./pipe/docker_engine}")
    private String dockerHost;

    @Value("${docker.api.version:1.41}")
    private String dockerApiVersion;

    @Bean
    public DockerClient dockerClient() {
        try {
            log.info("🐳 Initializing Docker Client...");
            log.info("Docker Host: {}", dockerHost);
            log.info("Docker API Version: {}", dockerApiVersion);

            DefaultDockerClientConfig config = DefaultDockerClientConfig.createDefaultConfigBuilder()
                    .withDockerHost(dockerHost)
                    .withDockerTlsVerify(false)
                    .withApiVersion(dockerApiVersion)
                    .build();

            // Dùng ZerodepDockerHttpClient để hỗ trợ cả Windows named pipe và Unix socket
            ZerodepDockerHttpClient httpClient = new ZerodepDockerHttpClient.Builder()
                    .dockerHost(URI.create(dockerHost))
                    .connectionTimeout(Duration.ofSeconds(10))
                    .responseTimeout(Duration.ofSeconds(30))
                    .build();

            DockerClient dockerClient = DockerClientImpl.getInstance(config, httpClient);

            // Test connection
            dockerClient.pingCmd().exec();
            log.info("✅ Docker Client initialized successfully");

            return dockerClient;
        } catch (Exception e) {
            log.error("❌ Failed to initialize Docker Client: {}", e.getMessage());
            log.error("💡 Make sure Docker is running and accessible");
            log.error("💡 Windows: Ensure Docker Desktop is running");
            log.error("💡 Linux/Mac: Check Docker daemon is running");
            throw new RuntimeException("Docker initialization failed: " + e.getMessage(), e);
        }
    }
}

