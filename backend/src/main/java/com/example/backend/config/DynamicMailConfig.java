package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class DynamicMailConfig {

    @Bean
    public JavaMailSender javaMailSender() {
        return new JavaMailSenderImpl();
    }

    public static JavaMailSender createMailSender(WebsiteSettings settings) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        
        // 设置 SMTP 配置
        mailSender.setHost(settings.getSmtpHost());
        mailSender.setPort(settings.getSmtpPort());
        mailSender.setUsername(settings.getSmtpUsername());
        mailSender.setPassword(settings.getSmtpPassword());
        
        // 设置属性
        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", String.valueOf(!settings.isSmtpSsl()));
        props.put("mail.smtp.ssl.enable", String.valueOf(settings.isSmtpSsl()));
        props.put("mail.smtp.ssl.protocols", "TLSv1.2");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        props.put("mail.smtp.socketFactory.fallback", "false");
        
        if (settings.isSmtpSsl()) {
            props.put("mail.smtp.socketFactory.port", String.valueOf(settings.getSmtpPort()));
        }
        
        return mailSender;
    }
}
