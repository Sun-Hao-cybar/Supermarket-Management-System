package com.supermarket.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.backend.config.AgentConfig;
import com.supermarket.backend.dto.ChatRequest;
import com.supermarket.backend.dto.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AgentService {

    @Autowired
    private AgentConfig agentConfig;

    private static final Logger log = LoggerFactory.getLogger(AgentService.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String SYSTEM_PROMPT =
        "你是超市进销存管理系统的AI助手，形象是一只白色小猫。" +
        "你只回答与进销存系统操作相关的问题。" +
        "回答风格：亲切、简洁、带'喵~'语气词。" +
        "遇到无关问题，友好拒绝并引导用户回到系统操作话题。" +
        "系统包含以下模块：员工管理、会员管理、商品管理、采购管理（含采购退货）、供应商管理。" +
        "用户角色分为普通用户和管理员（一号/二号/三号管理员），不同角色有不同的操作权限。";

    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();
        try {
            // 构建 messages 数组
            List<Map<String, String>> messages = new ArrayList<>();

            // system prompt
            Map<String, String> systemMsg = new HashMap<>();
            systemMsg.put("role", "system");
            systemMsg.put("content", SYSTEM_PROMPT);
            messages.add(systemMsg);

            // 历史对话
            if (request.getHistory() != null) {
                for (Map<String, String> h : request.getHistory()) {
                    messages.add(new HashMap<>(h));
                }
            }

            // 当前用户消息
            Map<String, String> userMsg = new HashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", request.getMessage());
            messages.add(userMsg);

            // 构建请求体
            Map<String, Object> body = new HashMap<>();
            body.put("model", agentConfig.getModel());
            body.put("messages", messages);
            body.put("temperature", 0.7);
            body.put("max_tokens", 1000);

            // 发送请求
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + agentConfig.getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> resp = restTemplate.postForEntity(
                agentConfig.getApiUrl(), entity, String.class);

            // 解析响应
            JsonNode root = objectMapper.readTree(resp.getBody());
            JsonNode choices = root.get("choices");
            if (choices != null && choices.isArray() && choices.size() > 0) {
                String content = choices.get(0).path("message").path("content").asText();
                response.setReply(content != null && !content.isEmpty() ? content : "喵~ AI 返回了空内容，请换个问题试试~");
            } else {
                log.error("DeepSeek API response missing choices array: {}", resp.getBody());
                response.setReply("喵~ 抱歉，AI 服务返回格式异常，请稍后再试~");
            }
        } catch (HttpClientErrorException e) {
            log.error("DeepSeek API returned error status: {} {}", e.getStatusCode(), e.getStatusText());
            response.setReply("喵~ 抱歉，AI 服务暂时不可用，请稍后再试~");
        } catch (Exception e) {
            log.error("DeepSeek API call failed", e);
            response.setReply("喵~ 抱歉，AI 服务暂时不可用，请稍后再试~");
        }
        return response;
    }
}
