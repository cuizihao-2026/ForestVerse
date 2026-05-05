package com.example.backend.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    String chat(String userMessage, String articleContent, List<Map<String, String>> history);
    void streamChat(String userMessage, String articleContent, List<Map<String, String>> history, StreamResponseHandler handler);
    Map<String, Object> queryBalance();
    
    interface StreamResponseHandler {
        void onChunk(String chunk);
        void onComplete();
        void onError(Throwable error);
    }
}
