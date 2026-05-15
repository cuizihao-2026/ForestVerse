package com.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    String chat(String userMessage, String articleContent, List<Map<String, String>> history);
    void streamChat(String userMessage, String articleContent, List<Map<String, String>> history, StreamResponseHandler handler);
    Map<String, Object> queryBalance();
    
    // 写作助手相关方法
    String writingAssist(String taskType, String title, String content, String userPrompt);
    void streamWritingAssist(String taskType, String title, String content, String userPrompt, StreamResponseHandler handler);
    
    interface StreamResponseHandler {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
}
