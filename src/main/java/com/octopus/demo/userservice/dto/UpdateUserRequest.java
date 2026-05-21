package com.octopus.demo.userservice.dto;

import jakarta.validation.constraints.Email;

public class UpdateUserRequest {

    private String username;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer age;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }
}