package com.octopus.demo.userservice.config;

import com.octopus.demo.common.util.JwtConfig;
import com.octopus.demo.common.util.JwtUtil;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfiguration {

    private static final Logger log = LoggerFactory.getLogger(JwtConfiguration.class);

    private final JwtProperties properties;

    public JwtConfiguration(JwtProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void configureJwt() {
        String secretKey = properties.getSecretKey();
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("octopus.jwt.secret-key is not configured; keeping JwtUtil default development key");
            secretKey = null;
        }
        JwtUtil.update(new JwtConfig(secretKey, properties.getExpirationDays()));
    }
}
