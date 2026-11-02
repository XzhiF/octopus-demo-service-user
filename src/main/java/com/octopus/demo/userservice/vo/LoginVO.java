package com.octopus.demo.userservice.vo;

public class LoginVO {

    private final String token;
    private final String tokenType;
    private final long expiresInSeconds;
    private final UserVO user;

    public LoginVO(String token, String tokenType, long expiresInSeconds, UserVO user) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresInSeconds = expiresInSeconds;
        this.user = user;
    }

    public String getToken() {
        return token;
    }

    public String getTokenType() {
        return tokenType;
    }

    public long getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public UserVO getUser() {
        return user;
    }
}
