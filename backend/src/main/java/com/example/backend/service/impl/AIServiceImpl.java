package com.example.backend.service.impl;

import com.example.backend.config.WebsiteSettings;
import com.example.backend.service.AIService;
import com.example.backend.service.SettingsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AIServiceImpl implements AIService {

    private static final Logger logger = LoggerFactory.getLogger(AIServiceImpl.class);

    @Autowired
    private SettingsService settingsService;

    @Autowired
    private RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public static class AuditResult {
        public boolean approved;
        public String reason;

        public AuditResult(boolean approved, String reason) {
            this.approved = approved;
            this.reason = reason;
        }
    }

    public AuditResult auditArticle(String title, String content) {
        WebsiteSettings settings = settingsService.getSettings();
        
        if (!settings.isAiArticleAuditEnabled()) {
            return new AuditResult(false, "AI审核功能未启用");
        }
        
        if (settings.getAiApiKey() == null || settings.getAiApiKey().isEmpty()) {
            return new AuditResult(false, "AI API密钥未配置");
        }
        
        try {
            String provider = settings.getAiProvider();
            String baseUrl = settings.getAiBaseUrl();
            String apiKey = settings.getAiApiKey();
            String model = settings.getAiModel();
            
            if (provider == null || provider.isEmpty()) {
                provider = "deepseek";
            }
            
            String prompt = buildAuditPrompt(title, content);
            
            String response;
            if ("anthropic".equals(provider)) {
                response = callAnthropicApiForAudit(prompt, baseUrl, apiKey, model);
            } else {
                response = callOpenAiCompatibleApiForAudit(prompt, baseUrl, apiKey, model);
            }
            
            return parseAuditResponse(response);
            
        } catch (Exception e) {
            logger.error("AI服务调用失败: " + e.getMessage(), e);
            return new AuditResult(false, "AI审核失败: " + e.getMessage());
        }
    }

    public AuditResult auditComment(String content) {
        WebsiteSettings settings = settingsService.getSettings();
        
        if (!settings.isAiCommentAuditEnabled()) {
            return new AuditResult(false, "AI评论审核功能未启用");
        }
        
        if (settings.getAiApiKey() == null || settings.getAiApiKey().isEmpty()) {
            return new AuditResult(false, "AI API密钥未配置");
        }
        
        try {
            String provider = settings.getAiProvider();
            String baseUrl = settings.getAiBaseUrl();
            String apiKey = settings.getAiApiKey();
            String model = settings.getAiModel();
            
            if (provider == null || provider.isEmpty()) {
                provider = "deepseek";
            }
            
            String prompt = buildCommentAuditPrompt(content);
            
            String response;
            if ("anthropic".equals(provider)) {
                response = callAnthropicApiForAudit(prompt, baseUrl, apiKey, model);
            } else {
                response = callOpenAiCompatibleApiForAudit(prompt, baseUrl, apiKey, model);
            }
            
            return parseAuditResponse(response);
            
        } catch (Exception e) {
            logger.error("AI服务调用失败: " + e.getMessage(), e);
            return new AuditResult(false, "AI评论审核失败: " + e.getMessage());
        }
    }

    private String buildAuditPrompt(String title, String content) {
        WebsiteSettings settings = settingsService.getSettings();
        String customPrompt = settings.getAiArticleAuditPrompt();
        
        if (customPrompt != null && !customPrompt.isEmpty()) {
            return customPrompt.replace("{title}", title).replace("{content}", content);
        }
        
        return "你是一名专业、严格、公正的文章审核员，请按照社区规则对以下文章进行审核。\n\n" +
            "【重要说明】\n" +
            "- 文章内容中包含HTML标签、富文本编辑器代码、图片标签等是完全正常的，这不属于违规内容\n" +
            "- 请仅根据文章的实际文字内容进行审核判断，不要因为存在格式标签而拒绝\n\n" +
            "【文章标题】\n" + title + "\n\n" +
            "【文章内容】\n" + content + "\n\n" +
            "【审核维度】请逐一检查以下 4 项，必须全部达标才算合格：\n" +
            "1. 无违规内容：不含色情、暴力、政治敏感、虚假信息、违法违规内容\n" +
            "2. 无广告垃圾：不含营销广告、引流信息、无意义灌水内容\n" +
            "3. 内容质量合格：逻辑清晰、表达通顺、具备实际价值；无价值水文、凑字数内容一律判定不合格\n" +
            "4. 无其他违规：不违反平台社区规范\n\n" +
            "【返回格式】严格遵守，不得修改格式：\n" +
            "- 文章完全合格 → 仅返回：通过\n" +
            "- 文章不合格 → 固定返回：拒绝: 具体不合格原因（简洁明确）";
    }

    private String buildCommentAuditPrompt(String content) {
        WebsiteSettings settings = settingsService.getSettings();
        String customPrompt = settings.getAiCommentAuditPrompt();
        
        if (customPrompt != null && !customPrompt.isEmpty()) {
            return customPrompt.replace("{content}", content);
        }
        
        return "你是一名专业、严格、公正的评论审核员，请按照社区规则对以下评论进行审核。\n\n" +
            "【评论内容】\n" + content + "\n\n" +
            "【审核维度】请逐一检查以下 3 项，必须全部达标才算合格：\n" +
            "1. 无违规内容：不含色情、暴力、政治敏感、虚假信息、违法违规内容\n" +
            "2. 无广告垃圾：不含营销广告、引流信息、无意义灌水内容\n" +
            "3. 无其他违规：不违反平台社区规范\n\n" +
            "【返回格式】严格遵守，不得修改格式：\n" +
            "- 评论完全合格 → 仅返回：通过\n" +
            "- 评论不合格 → 固定返回：拒绝: 具体不合格原因（简洁明确）";
    }

    private AuditResult parseAuditResponse(String response) {
        if (response == null || response.isEmpty()) {
            return new AuditResult(false, "AI返回结果为空");
        }
        
        String trimmed = response.trim();
        
        if (trimmed.equals("通过")) {
            return new AuditResult(true, "");
        } else if (trimmed.startsWith("拒绝")) {
            String reason = trimmed;
            if (reason.startsWith("拒绝:")) {
                reason = reason.substring("拒绝:".length()).trim();
            } else if (reason.startsWith("拒绝：")) {
                reason = reason.substring("拒绝：".length()).trim();
            }
            return new AuditResult(false, reason);
        } else {
            return new AuditResult(false, "AI返回格式错误: " + response);
        }
    }

    private String callOpenAiCompatibleApiForAudit(String prompt, String baseUrl, String apiKey, String model) throws Exception {
        String url = baseUrl + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的文章审核员，只返回审核结果，不要有其他解释。");
        messages.add(systemMessage);
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    return message.get("content").asText();
                }
            }
            throw new Exception("AI响应格式错误");
        } else {
            throw new Exception("AI服务请求失败，状态码: " + response.getStatusCode());
        }
    }

    private String callAnthropicApiForAudit(String prompt, String baseUrl, String apiKey, String model) throws Exception {
        String url = baseUrl + "/messages";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", prompt);
        messages.add(userMsg);
        
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("system", "你是一个专业的文章审核员，只返回审核结果，不要有其他解释。");
        requestBody.put("max_tokens", 1024);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode content = responseJson.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                if (firstContent != null && firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }
            throw new Exception("AI响应格式错误");
        } else {
            throw new Exception("AI服务请求失败，状态码: " + response.getStatusCode());
        }
    }

    @Override
    public String chat(String userMessage, String articleContent, List<Map<String, String>> history) {
        WebsiteSettings settings = settingsService.getSettings();
        
        if (!settings.isAiReadingEnabled()) {
            return "AI功能未启用，请联系管理员配置";
        }
        
        if (settings.getAiApiKey() == null || settings.getAiApiKey().isEmpty()) {
            return "AI API密钥未配置，请联系管理员配置";
        }
        
        try {
            String provider = settings.getAiProvider();
            String baseUrl = settings.getAiBaseUrl();
            String apiKey = settings.getAiApiKey();
            String model = settings.getAiModel();
            
            if (provider == null || provider.isEmpty()) {
                provider = "deepseek";
            }
            
            if ("anthropic".equals(provider)) {
                return callAnthropicApi(userMessage, articleContent, history, baseUrl, apiKey, model);
            } else {
                return callOpenAiCompatibleApi(userMessage, articleContent, history, baseUrl, apiKey, model);
            }
            
        } catch (Exception e) {
            logger.error("AI服务调用失败: " + e.getMessage(), e);
            return "AI服务调用失败: " + e.getMessage();
        }
    }
    
    private String callOpenAiCompatibleApi(String userMessage, String articleContent, 
            List<Map<String, String>> history, String baseUrl, String apiKey, String model) throws Exception {
        
        String url = baseUrl + "/chat/completions";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        WebsiteSettings settings = settingsService.getSettings();
        String systemPrompt = settings.getAiSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = "你是一个文章阅读助手，帮助用户理解文章内容。请根据用户的问题和提供的文章内容给出回答。在回答中可以使用 Markdown 格式来增强可读性，比如 **加粗**、*斜体*、列表等。";
        }
        if (articleContent != null && !articleContent.isEmpty()) {
            systemPrompt += "\n\n以下是文章内容：\n" + articleContent;
        }
        
        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);
        messages.add(systemMessage);
        
        // 添加历史消息
        if (history != null && !history.isEmpty()) {
            // 转换角色：ai -> assistant
            List<Map<String, String>> convertedHistory = new ArrayList<>();
            for (Map<String, String> msg : history) {
                Map<String, String> newMsg = new HashMap<>(msg);
                String role = newMsg.get("role");
                if ("ai".equals(role)) {
                    newMsg.put("role", "assistant");
                }
                convertedHistory.add(newMsg);
            }
            messages.addAll(convertedHistory);
        }
        
        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode choices = responseJson.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                JsonNode message = choices.get(0).get("message");
                if (message != null) {
                    String content = message.get("content").asText();
                    return content;
                }
            }
            return "AI响应格式错误";
        } else {
            return "AI服务请求失败，状态码: " + response.getStatusCode();
        }
    }
    
    private String callAnthropicApi(String userMessage, String articleContent, 
            List<Map<String, String>> history, String baseUrl, String apiKey, String model) throws Exception {
        
        String url = baseUrl + "/messages";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");
        
        List<Map<String, String>> messages = new ArrayList<>();
        
        WebsiteSettings settings = settingsService.getSettings();
        String systemPrompt = settings.getAiSystemPrompt();
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = "你是一个文章阅读助手，帮助用户理解文章内容。请根据用户的问题和提供的文章内容给出回答。在回答中可以使用 Markdown 格式来增强可读性，比如 **加粗**、*斜体*、列表等。";
        }
        if (articleContent != null && !articleContent.isEmpty()) {
            systemPrompt += "\n\n以下是文章内容：\n" + articleContent;
        }
        
        // 添加历史消息
        if (history != null && !history.isEmpty()) {
            for (Map<String, String> msg : history) {
                Map<String, String> newMsg = new HashMap<>(msg);
                String role = newMsg.get("role");
                if ("ai".equals(role)) {
                    newMsg.put("role", "assistant");
                }
                messages.add(newMsg);
            }
        }
        
        // 添加当前用户消息
        Map<String, String> userMsg = new HashMap<>();
        userMsg.put("role", "user");
        userMsg.put("content", userMessage);
        messages.add(userMsg);
        
        // 构建请求体
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("messages", messages);
        requestBody.put("system", systemPrompt);
        requestBody.put("max_tokens", 4096);
        
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);
        
        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        
        if (response.getStatusCode() == HttpStatus.OK) {
            JsonNode responseJson = objectMapper.readTree(response.getBody());
            JsonNode content = responseJson.get("content");
            if (content != null && content.isArray() && content.size() > 0) {
                JsonNode firstContent = content.get(0);
                if (firstContent != null && firstContent.has("text")) {
                    return firstContent.get("text").asText();
                }
            }
            return "AI响应格式错误";
        } else {
            return "AI服务请求失败，状态码: " + response.getStatusCode();
        }
    }

    @Override
    public Map<String, Object> queryBalance() {
        Map<String, Object> result = new HashMap<>();
        WebsiteSettings settings = settingsService.getSettings();
        
        if (settings.getAiApiKey() == null || settings.getAiApiKey().isEmpty()) {
            result.put("success", false);
            result.put("message", "AI API密钥未配置");
            return result;
        }
        
        try {
            String provider = settings.getAiProvider();
            String baseUrl = settings.getAiBaseUrl();
            String apiKey = settings.getAiApiKey();
            
            if (provider == null || provider.isEmpty()) {
                provider = "deepseek";
            }
            
            Map<String, Object> balanceResult = queryBalanceByProvider(provider, baseUrl, apiKey);
            result.putAll(balanceResult);
            
        } catch (Exception e) {
            logger.error("AI服务调用失败: " + e.getMessage(), e);
            result.put("success", false);
            result.put("message", "查询余额失败: " + e.getMessage());
        }
        
        return result;
    }
    
    private Map<String, Object> queryBalanceByProvider(String provider, String baseUrl, String apiKey) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        try {
            String url;
            HttpHeaders headers = new HttpHeaders();
            
            if ("deepseek".equals(provider)) {
                // DeepSeek 余额查询
                url = "https://api.deepseek.com/user/balance";
                headers.set("Authorization", "Bearer " + apiKey);
            } else if ("kimi".equals(provider)) {
                // Kimi (Moonshot) 余额查询
                url = "https://api.moonshot.cn/v1/users/me/balance";
                headers.set("Authorization", "Bearer " + apiKey);
            } else if ("qwen".equals(provider)) {
                // 千问 (阿里云) 余额查询
                url = "https://dashscope.aliyuncs.com/api/v1/service/stat";
                headers.set("Authorization", "Bearer " + apiKey);
            } else {
                // 其他提供商
                url = baseUrl + "/user/balance";
                headers.set("Authorization", "Bearer " + apiKey);
            }
            
            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
            
            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode responseJson = objectMapper.readTree(response.getBody());
                
                result.put("success", true);
                result.put("provider", provider);
                result.put("rawResponse", response.getBody());
                
                // 解析常见的余额格式
                if (responseJson.has("is_available")) {
                    result.put("isAvailable", responseJson.get("is_available").asBoolean());
                }
                if (responseJson.has("balance_infos")) {
                    result.put("balanceInfos", objectMapper.convertValue(responseJson.get("balance_infos"), Object.class));
                    result.put("balance_infos", objectMapper.convertValue(responseJson.get("balance_infos"), Object.class));
                }
                if (responseJson.has("available_balance")) {
                    result.put("availableBalance", responseJson.get("available_balance").asText());
                }
                // Kimi 格式
                if (responseJson.has("data") && responseJson.get("data").has("available_balance")) {
                    result.put("availableBalance", responseJson.get("data").get("available_balance").asText());
                }
                if (responseJson.has("data") && responseJson.get("data").has("balance")) {
                    result.put("availableBalance", responseJson.get("data").get("balance").asText());
                }
                
            } else {
                result.put("success", false);
                result.put("message", "查询失败，状态码: " + response.getStatusCode());
            }
            
        } catch (Exception e) {
            // 如果通用方式失败，尝试一些其他可能的方式
            try {
                return queryBalanceAlternative(provider, baseUrl, apiKey);
            } catch (Exception e2) {
                result.put("success", false);
                result.put("message", "不支持该提供商的余额查询或API密钥无效: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    private Map<String, Object> queryBalanceAlternative(String provider, String baseUrl, String apiKey) throws Exception {
        Map<String, Object> result = new HashMap<>();
        
        // 尝试其他可能的API端点
        String[] possibleUrls;
        
        if ("kimi".equals(provider)) {
            possibleUrls = new String[]{
                baseUrl + "/users/me",
                baseUrl + "/billing/credit_grants",
                "https://api.moonshot.cn/v1/users/me"
            };
        } else if ("qwen".equals(provider)) {
            possibleUrls = new String[]{
                "https://dashscope.aliyuncs.com/api/v1/service/stat"
            };
        } else {
            possibleUrls = new String[]{
                baseUrl + "/user/balance",
                baseUrl + "/billing/credit_grants",
                baseUrl + "/dashboard/billing/usage"
            };
        }
        
        for (String url : possibleUrls) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set("Authorization", "Bearer " + apiKey);
                
                HttpEntity<Void> entity = new HttpEntity<>(headers);
                ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);
                
                if (response.getStatusCode() == HttpStatus.OK) {
                    result.put("success", true);
                    result.put("provider", provider);
                    result.put("rawResponse", response.getBody());
                    
                    // 尝试解析响应
                    JsonNode responseJson = objectMapper.readTree(response.getBody());
                    if (responseJson.has("total_available")) {
                        result.put("availableBalance", responseJson.get("total_available").asText());
                    }
                    if (responseJson.has("data")) {
                        result.put("data", objectMapper.convertValue(responseJson.get("data"), Object.class));
                    }
                    
                    return result;
                }
            } catch (Exception e) {
                // 继续尝试下一个URL
            }
        }
        
        result.put("success", false);
        result.put("message", "无法找到可用的余额查询接口");
        return result;
    }
    
    @Override
    public void streamChat(String userMessage, String articleContent, List<Map<String, String>> history, StreamResponseHandler handler) {
        try {
            WebsiteSettings settings = settingsService.getSettings();
            
            if (!settings.isAiReadingEnabled()) {
                handler.onError(new RuntimeException("AI功能未启用"));
                return;
            }
            
            if (settings.getAiApiKey() == null || settings.getAiApiKey().isEmpty()) {
                handler.onError(new RuntimeException("AI API密钥未配置"));
                return;
            }
            
            String provider = settings.getAiProvider();
            String baseUrl = settings.getAiBaseUrl();
            String apiKey = settings.getAiApiKey();
            String model = settings.getAiModel();
            
            if (provider == null || provider.isEmpty()) {
                provider = "deepseek";
            }
            
            if ("anthropic".equals(provider)) {
                streamAnthropicApi(userMessage, articleContent, history, baseUrl, apiKey, model, handler);
            } else {
                streamOpenAiCompatibleApi(userMessage, articleContent, history, baseUrl, apiKey, model, handler);
            }
            
        } catch (Exception e) {
            handler.onError(e);
        }
    }
    
    private void streamOpenAiCompatibleApi(String userMessage, String articleContent, 
                                           List<Map<String, String>> history, String baseUrl, String apiKey, 
                                           String model, StreamResponseHandler handler) {
        try {
            String url = baseUrl + "/chat/completions";
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            WebsiteSettings settings = settingsService.getSettings();
            String systemPrompt = settings.getAiSystemPrompt();
            if (systemPrompt == null || systemPrompt.isEmpty()) {
                systemPrompt = "你是一个文章阅读助手，帮助用户理解文章内容。请根据用户的问题和提供的文章内容给出回答。在回答中可以使用 Markdown 格式来增强可读性，比如 **加粗**、*斜体*、列表等。";
            }
            if (articleContent != null && !articleContent.isEmpty()) {
                systemPrompt += "\n\n以下是文章内容：\n" + articleContent;
            }
            
            Map<String, String> systemMessage = new HashMap<>();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
            messages.add(systemMessage);
            
            // 添加历史消息
            if (history != null && !history.isEmpty()) {
                List<Map<String, String>> convertedHistory = new ArrayList<>();
                for (Map<String, String> msg : history) {
                    Map<String, String> newMsg = new HashMap<>(msg);
                    String role = newMsg.get("role");
                    if ("ai".equals(role)) {
                        newMsg.put("role", "assistant");
                    }
                    convertedHistory.add(newMsg);
                }
                messages.addAll(convertedHistory);
            }
            
            // 添加当前用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            // 构建请求体，启用流式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("stream", true);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), "UTF-8"))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }
                                    try {
                                        JsonNode json = objectMapper.readTree(data);
                                        JsonNode choices = json.get("choices");
                                        if (choices != null && choices.isArray() && choices.size() > 0) {
                                            JsonNode delta = choices.get(0).get("delta");
                                            if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                                                String content = delta.get("content").asText();
                                                if (!content.isEmpty()) {
                                                    handler.onChunk(content);
                                                }
                                            }
                                        }
                                    } catch (Exception e) {
                                        // 忽略解析错误
                                    }
                                }
                            }
                            handler.onComplete();
                        } catch (Exception e) {
                            handler.onError(e);
                        }
                    })
                    .exceptionally(throwable -> {
                        handler.onError(throwable);
                        return null;
                    });
            
        } catch (Exception e) {
            handler.onError(e);
        }
    }
    
    private void streamAnthropicApi(String userMessage, String articleContent, 
                                    List<Map<String, String>> history, String baseUrl, String apiKey, 
                                    String model, StreamResponseHandler handler) {
        try {
            String url = baseUrl + "/messages";
            
            List<Map<String, String>> messages = new ArrayList<>();
            
            WebsiteSettings settings = settingsService.getSettings();
            String systemPrompt = settings.getAiSystemPrompt();
            if (systemPrompt == null || systemPrompt.isEmpty()) {
                systemPrompt = "你是一个文章阅读助手，帮助用户理解文章内容。请根据用户的问题和提供的文章内容给出回答。在回答中可以使用 Markdown 格式来增强可读性，比如 **加粗**、*斜体*、列表等。";
            }
            if (articleContent != null && !articleContent.isEmpty()) {
                systemPrompt += "\n\n以下是文章内容：\n" + articleContent;
            }
            
            // 添加历史消息
            if (history != null && !history.isEmpty()) {
                for (Map<String, String> msg : history) {
                    Map<String, String> newMsg = new HashMap<>(msg);
                    String role = newMsg.get("role");
                    if ("ai".equals(role)) {
                        newMsg.put("role", "assistant");
                    }
                    messages.add(newMsg);
                }
            }
            
            // 添加当前用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);
            messages.add(userMsg);
            
            // 构建请求体，启用流式
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("messages", messages);
            requestBody.put("system", systemPrompt);
            requestBody.put("max_tokens", 4096);
            requestBody.put("stream", true);
            
            String requestJson = objectMapper.writeValueAsString(requestBody);
            
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson))
                    .build();
            
            client.sendAsync(request, HttpResponse.BodyHandlers.ofInputStream())
                    .thenAccept(response -> {
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), "UTF-8"))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (line.startsWith("data: ")) {
                                    String data = line.substring(6);
                                    try {
                                        JsonNode json = objectMapper.readTree(data);
                                        String type = json.has("type") ? json.get("type").asText() : "";
                                        if ("content_block_delta".equals(type)) {
                                            JsonNode delta = json.get("delta");
                                            if (delta != null && delta.has("text") && !delta.get("text").isNull()) {
                                                String text = delta.get("text").asText();
                                                if (!text.isEmpty()) {
                                                    handler.onChunk(text);
                                                }
                                            }
                                        } else if ("message_stop".equals(type)) {
                                            break;
                                        }
                                    } catch (Exception e) {
                                        // 忽略解析错误
                                    }
                                }
                            }
                            handler.onComplete();
                        } catch (Exception e) {
                            handler.onError(e);
                        }
                    })
                    .exceptionally(throwable -> {
                        handler.onError(throwable);
                        return null;
                    });
            
        } catch (Exception e) {
            handler.onError(e);
        }
    }
}
