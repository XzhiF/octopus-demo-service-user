package com.octopus.demo.userservice.vo;

import com.octopus.demo.common.util.MaskUtils;
import com.octopus.demo.userservice.model.User;
import java.time.LocalDateTime;

public class UserVO {
    private Long id;
    private String username;
    private String nickname;
    private String email;
    private Integer age;
    private String phone;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public UserVO() {}

    public static UserVO from(User user) {
        UserVO vo = new UserVO();
        vo.id = user.getId();
        vo.username = user.getUsername();
        vo.nickname = MaskUtils.maskNickname(user.getNickname());
        vo.email = MaskUtils.maskEmail(user.getEmail());
        vo.age = user.getAge();
        vo.phone = MaskUtils.maskPhone(user.getPhone());
        vo.status = user.getStatus();
        vo.createdAt = user.getCreatedAt();
        vo.updatedAt = user.getUpdatedAt();
        return vo;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public Integer getAge() { return age; }
    public void setAge(Integer age) { this.age = age; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
