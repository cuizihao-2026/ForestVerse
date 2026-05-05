package com.example.backend.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Random;

public class CaptchaUtils {

    private static final Logger logger = LoggerFactory.getLogger(CaptchaUtils.class);
    // 验证码字符集
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    // 图片宽度
    private static final int WIDTH = 160;
    // 图片高度
    private static final int HEIGHT = 60;

    // 生成验证码
    public static String generateCode(int codeLength) {
        Random random = new Random();
        StringBuilder code = new StringBuilder();
        for (int i = 0; i < codeLength; i++) {
            code.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return code.toString();
    }

    // 生成验证码图片（带配置）
    public static String generateImage(String code, int noiseCount, int lineCount, boolean rotateEnabled) {
        BufferedImage image = new BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();
        Random random = new Random();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 设置背景色
        g2d.setColor(getRandomColor(200, 250));
        g2d.fillRect(0, 0, WIDTH, HEIGHT);

        // 设置字体
        g2d.setFont(new Font("Arial", Font.BOLD, 30));

        // 绘制干扰线
        if (lineCount > 0) {
            for (int i = 0; i < lineCount; i++) {
                g2d.setColor(getRandomColor(160, 200));
                int x1 = random.nextInt(WIDTH);
                int y1 = random.nextInt(HEIGHT);
                int x2 = random.nextInt(WIDTH);
                int y2 = random.nextInt(HEIGHT);
                g2d.drawLine(x1, y1, x2, y2);
            }
        }

        // 添加噪点
        if (noiseCount > 0) {
            for (int i = 0; i < noiseCount; i++) {
                g2d.setColor(getRandomColor(120, 240));
                int x = random.nextInt(WIDTH);
                int y = random.nextInt(HEIGHT);
                int size = random.nextInt(4) + 1;
                if (random.nextBoolean()) {
                    g2d.fillOval(x, y, size, size);
                } else {
                    g2d.fillRect(x, y, size, size);
                }
            }
        }

        // 绘制验证码
        for (int i = 0; i < code.length(); i++) {
            g2d.setColor(getRandomColor(20, 120));
            
            if (rotateEnabled) {
                // 随机旋转文字
                AffineTransform old = g2d.getTransform();
                double angle = (random.nextDouble() - 0.5) * Math.PI / 4; // ±22.5度
                int x = 20 + i * (WIDTH - 40) / code.length();
                int y = 40;
                g2d.rotate(angle, x, y);
                g2d.drawString(String.valueOf(code.charAt(i)), x, y);
                g2d.setTransform(old);
            } else {
                g2d.drawString(String.valueOf(code.charAt(i)), 20 + i * (WIDTH - 40) / code.length(), 40);
            }
        }

        // 释放资源
        g2d.dispose();

        // 将图片转换为Base64
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "png", baos);
            byte[] bytes = baos.toByteArray();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            logger.error("验证码生成失败: " + e.getMessage(), e);
            return null;
        } finally {
            try {
                baos.close();
            } catch (IOException e) {
                logger.error("验证码生成失败: " + e.getMessage(), e);
            }
        }
    }

    // 兼容旧方法
    public static String generateCode() {
        return generateCode(6);
    }

    // 兼容旧方法
    public static String generateImage(String code) {
        return generateImage(code, 150, 10, false);
    }

    // 获取随机颜色
    private static Color getRandomColor(int min, int max) {
        Random random = new Random();
        return new Color(min + random.nextInt(max - min), min + random.nextInt(max - min), min + random.nextInt(max - min));
    }
}
