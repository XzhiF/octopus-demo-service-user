package com.octopus.demo.userservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "octopus.jwt")
public class JwtProperties {

    private String secretKey = "";
    private long expirationDays = 30;

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public long getExpirationDays() {
        return expirationDays;
    }

    public void setExpirationDays(long expirationDays) {
        this.expirationDays = expirationDays;
    }
}
