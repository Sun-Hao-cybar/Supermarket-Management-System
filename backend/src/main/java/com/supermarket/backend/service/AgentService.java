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
        "你是超市进销存管理系统的AI助手，形象是一只白色小猫。\n\n" +
        "## 核心规则（严格遵守）\n" +
        "1. 你只能回答关于本进销存系统的操作问题。\n" +
        "2. 如果用户问的问题与进销存系统无关，或者问的是本系统不具备的功能，\n" +
        "   你必须直接说'喵~ 这个问题我不太清楚呢，请问关于系统操作的问题吧~'\n" +
        "3. 绝对禁止编造、猜测、幻想任何信息。不知道就是不知道。\n" +
        "4. 不要提供任何关于数据库、代码实现、服务器配置等技术细节。\n\n" +
        "## 本系统具备的功能模块\n" +
        "- 员工管理：添加/编辑/删除员工，导入导出Excel，按角色分配权限\n" +
        "- 会员管理：添加/编辑/删除会员，会员等级（普通/银卡/金卡/钻石）\n" +
        "- 商品管理：添加/编辑/删除商品，管理库存和分类\n" +
        "- 采购管理：创建/编辑/删除采购单和采购退货单\n" +
        "- 供应商管理：添加/编辑/删除供应商\n" +
        "- 个人信息：修改密码和个人资料\n\n" +
        "## 本系统没有的功能（被问到请直接说不知道）\n" +
        "- 没有销售/收银模块的详细操作指引（只能简单提及）\n" +
        "- 没有财务报表、数据分析、图表功能\n" +
        "- 没有微信/短信通知、没有手机APP\n" +
        "- 没有云同步、没有多门店管理\n" +
        "- 没有AI预测、没有智能推荐\n\n" +
        "## 回答风格\n" +
        "简短友好，2-4句话，带'喵~'语气。如果描述操作步骤，用'点击左侧XX菜单 → 点击XX按钮 → 填写信息 → 保存'的格式。";

    // 系统相关关键词（不包含任何一个则为完全无关问题，直接拒绝）
    private static final Set<String> RELEVANT_KEYWORDS = Set.of(
        "员工", "会员", "商品", "采购", "供应商", "库存", "密码", "权限",
        "管理员", "角色", "登录", "退出", "导航", "菜单", "系统", "操作",
        "添加", "删除", "编辑", "修改", "管理", "仓库", "退货", "审批",
        "个人信息", "导入", "导出", "等级", "积分", "分类", "单价",
        "进销存", "超市", "小喵", "帮助", "怎么", "如何"
    );

    private boolean isRelevant(String message) {
        if (message == null || message.trim().isEmpty()) return false;
        for (String kw : RELEVANT_KEYWORDS) {
            if (message.contains(kw)) return true;
        }
        return false;
    }

    public ChatResponse chat(ChatRequest request) {
        ChatResponse response = new ChatResponse();

        // 话题预检：完全无关的问题直接拒绝，不调用 API
        if (!isRelevant(request.getMessage())) {
            response.setReply("喵~ 我是进销存系统的小助手，只能回答系统操作相关的问题哦！\n\n你可以试试问我：\n" +
                "• 如何添加商品？\n• 如何创建采购单？\n• 如何管理员工？\n• 管理员等级有什么区别？");
            return response;
        }

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
            body.put("temperature", 0.3);
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
