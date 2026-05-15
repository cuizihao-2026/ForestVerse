package com.example.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class ResetPasswordRequest {
    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, max = 20, message = "用户名长度必须在3-20之间")
    private String username;
    
    @NotBlank(message = "邮箱不能为空")
    private String email;
    
    @NotBlank(message = "新密码不能为空")
    @Size(min = 8, message = "密码长度至少8位")
    private String newPassword;
    
    private String emailCaptcha;

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

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getEmailCaptcha() {
        return emailCaptcha;
    }

    public void setEmailCaptcha(String emailCaptcha) {
        this.emailCaptcha = emailCaptcha;
    }
}
