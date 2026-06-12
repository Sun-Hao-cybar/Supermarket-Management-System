# 白色小猫智能体 — 实现计划

> **For agentic workers:** 使用 superpowers:subagent-driven-development（推荐）或 superpowers:executing-plans 按任务逐步实现。步骤使用 `- [ ]` checkbox 追踪。

**Goal:** 为进销存系统添加浮动的白色小猫智能体，支持本地知识库匹配 + DeepSeek API 兜底的混合对话方案。

**Architecture:** 前端 Vue 3 组件（CatAgent 总控 → CatAvatar 小猫形象 + ChatDialog 对话面板 + SettingsPanel 设置面板），后端 Spring Boot 代理 DeepSeek API，知识库在前端做本地匹配。

**Tech Stack:** Vue 3 + Element Plus + Spring Boot + MyBatis + DeepSeek API (OpenAI兼容格式)

---

## 文件结构总览

### 后端新增
| 文件 | 职责 |
|------|------|
| `backend/src/main/java/com/supermarket/backend/dto/ChatRequest.java` | 对话请求 DTO |
| `backend/src/main/java/com/supermarket/backend/dto/ChatResponse.java` | 对话响应 DTO |
| `backend/src/main/java/com/supermarket/backend/config/AgentConfig.java` | DeepSeek 配置属性类 |
| `backend/src/main/java/com/supermarket/backend/service/AgentService.java` | DeepSeek API 调用逻辑 |
| `backend/src/main/java/com/supermarket/backend/controller/AgentController.java` | `/api/agent/*` REST 接口 |

### 后端修改
| 文件 | 修改内容 |
|------|---------|
| `backend/src/main/resources/application.yml` | 新增 `agent.deepseek.*` 配置段 |

### 前端新增
| 文件 | 职责 |
|------|------|
| `frontend/src/api/agent.js` | 前端 API 封装（调用 /api/agent/chat） |
| `frontend/src/utils/knowledgeBase.js` | 本地知识库（40+ QA + 匹配 + 不合规过滤） |
| `frontend/src/components/cat/CatAvatar.vue` | 小猫形象（视频播放 + 拖拽 + 状态动画） |
| `frontend/src/components/cat/ChatDialog.vue` | 对话面板（消息列表 + 输入框 + 快捷问题） |
| `frontend/src/components/cat/SettingsPanel.vue` | 模式设置面板 |
| `frontend/src/components/cat/CatAgent.vue` | 总控组件（状态管理 + 模式调度 + 子组件协调） |

### 前端修改
| 文件 | 修改内容 |
|------|---------|
| `frontend/src/views/Layout.vue` | `<script>` 中 import CatAgent，`<template>` 中引入 `<CatAgent />` |

---

## 实现顺序

```
Phase 1: 后端 (DTO → Config → Service → Controller)
Phase 2: 前端基础层 (knowledgeBase → agent.js)
Phase 3: 前端组件层 (CatAvatar → ChatDialog → SettingsPanel → CatAgent)
Phase 4: 集成 (Layout.vue 引入)
```

---

### Task 1: 创建请求/响应 DTO

**Files:**
- Create: `backend/src/main/java/com/supermarket/backend/dto/ChatRequest.java`
- Create: `backend/src/main/java/com/supermarket/backend/dto/ChatResponse.java`

- [ ] **Step 1: 创建 ChatRequest.java**

```java
package com.supermarket.backend.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
public class ChatRequest {
    private String message;
    private List<Map<String, String>> history;  // [{role, content}, ...]
}
```

- [ ] **Step 2: 创建 ChatResponse.java**

```java
package com.supermarket.backend.dto;

import lombok.Data;

@Data
public class ChatResponse {
    private String reply;
}
```

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/supermarket/backend/dto/ChatRequest.java backend/src/main/java/com/supermarket/backend/dto/ChatResponse.java
git commit -m "feat: 添加 Agent 对话 DTO"
```

---

### Task 2: 创建 AgentConfig 配置类

**Files:**
- Create: `backend/src/main/java/com/supermarket/backend/config/AgentConfig.java`
- Modify: `backend/src/main/resources/application.yml`

- [ ] **Step 1: 创建 AgentConfig.java**

```java
package com.supermarket.backend.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "agent.deepseek")
public class AgentConfig {
    private String apiKey;
    private String apiUrl = "https://api.deepseek.com/v1/chat/completions";
    private String model = "deepseek-chat";
}
```

- [ ] **Step 2: 在 application.yml 末尾追加配置**

```yaml
# DeepSeek AI Agent 配置
agent:
  deepseek:
    api-key: ${DEEPSEEK_API_KEY:sk-your-deepseek-api-key}
    api-url: https://api.deepseek.com/v1/chat/completions
    model: deepseek-chat
```

修改方式：在 `backend/src/main/resources/application.yml` 文件末尾追加以上 6 行。

- [ ] **Step 3: 提交**

```bash
git add backend/src/main/java/com/supermarket/backend/config/AgentConfig.java backend/src/main/resources/application.yml
git commit -m "feat: 添加 DeepSeek Agent 配置"
```

---

### Task 3: 创建 AgentService

**Files:**
- Create: `backend/src/main/java/com/supermarket/backend/service/AgentService.java`

- [ ] **Step 1: 创建 AgentService.java**

```java
package com.supermarket.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supermarket.backend.config.AgentConfig;
import com.supermarket.backend.dto.ChatRequest;
import com.supermarket.backend.dto.ChatResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
public class AgentService {

    @Autowired
    private AgentConfig agentConfig;

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
            String content = root.path("choices").get(0)
                .path("message").path("content").asText();

            response.setReply(content);
        } catch (Exception e) {
            response.setReply("喵~ 抱歉，AI 服务暂时不可用，请稍后再试~ (" + e.getMessage() + ")");
        }
        return response;
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/supermarket/backend/service/AgentService.java
git commit -m "feat: 添加 AgentService — DeepSeek API 调用"
```

---

### Task 4: 创建 AgentController

**Files:**
- Create: `backend/src/main/java/com/supermarket/backend/controller/AgentController.java`

- [ ] **Step 1: 创建 AgentController.java**

```java
package com.supermarket.backend.controller;

import com.supermarket.backend.common.Result;
import com.supermarket.backend.dto.ChatRequest;
import com.supermarket.backend.dto.ChatResponse;
import com.supermarket.backend.service.AgentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/agent")
public class AgentController {

    @Autowired
    private AgentService agentService;

    @PostMapping("/chat")
    public Result<ChatResponse> chat(@RequestBody ChatRequest request) {
        if (request.getMessage() == null || request.getMessage().trim().isEmpty()) {
            ChatResponse empty = new ChatResponse();
            empty.setReply("喵？你什么都没说呢~ 输入你的问题，我会尽力帮你！");
            return Result.success(empty);
        }
        ChatResponse response = agentService.chat(request);
        return Result.success(response);
    }
}
```

- [ ] **Step 2: 提交**

```bash
git add backend/src/main/java/com/supermarket/backend/controller/AgentController.java
git commit -m "feat: 添加 AgentController — /api/agent/chat 接口"
```

---

### Task 5: 创建前端 API 封装

**Files:**
- Create: `frontend/src/api/agent.js`

- [ ] **Step 1: 创建 agent.js**

```js
import request from '@/utils/request'

/**
 * 发送消息给 AI Agent
 * @param {string} message - 用户消息
 * @param {Array} history - 历史对话 [{role, content}, ...]
 * @returns {Promise<{reply: string}>}
 */
export function sendMessage(message, history = []) {
  return request.post('/agent/chat', { message, history })
}
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/api/agent.js
git commit -m "feat: 添加前端 Agent API 封装"
```

---

### Task 6: 创建本地知识库

**Files:**
- Create: `frontend/src/utils/knowledgeBase.js`

- [ ] **Step 1: 创建 knowledgeBase.js**

```js
/**
 * 本地知识库 — 进销存系统预设问答
 * 匹配策略：关键词共现 + Jaccard 相似度，阈值 > 0.3 命中
 * 未命中返回 null，由调用方转发后端 DeepSeek API
 */

const knowledgeBase = [
  // ==================== 员工管理 ====================
  {
    keywords: ['添加', '员工', '新增', '录入'],
    question: '如何添加员工？',
    answer: '喵~ 添加员工很简单！登录管理员账号后，点击左侧"员工管理"，在页面顶部点击"添加员工"按钮，填写员工的账号、密码、姓名、电话、工资、角色等信息后保存即可。需要注意的是，只有一号和三号管理员才有员工管理权限哦~'
  },
  {
    keywords: ['编辑', '修改', '员工', '信息'],
    question: '如何编辑员工信息？',
    answer: '喵~ 在"员工管理"页面，找到需要编辑的员工，点击操作列的"编辑"按钮，修改信息后保存就可以啦！'
  },
  {
    keywords: ['删除', '员工', '移除'],
    question: '如何删除员工？',
    answer: '喵~ 在"员工管理"页面的员工列表中，点击对应员工操作列的"删除"按钮，确认后即可删除。删除操作不可恢复，请谨慎操作哦！'
  },
  {
    keywords: ['管理员', '等级', '区别', '一号', '二号', '三号'],
    question: '管理员等级有什么区别？',
    answer: '喵~ 系统有三种管理员：\n 一号管理员：拥有全部权限（供应商+商品+员工+采购）\n 二号管理员：管理供应商和商品\n 三号管理员：管理员工和采购\n普通用户只能查看自己有权限的模块哦~'
  },
  {
    keywords: ['权限', '管理员权限'],
    question: '如何设置管理员权限？',
    answer: '喵~ 管理员权限在添加或编辑员工时设置。将员工的"角色"字段设为"管理员(1)"，系统会根据管理员的编号（adminLevel）自动分配对应权限。一号管理员还可以在注册时自动获得最高权限~'
  },
  {
    keywords: ['导入', '员工', '批量', 'excel'],
    question: '如何批量导入员工？',
    answer: '喵~ 在"员工管理"页面，点击"导入Excel"按钮，下载模板填写员工信息后上传即可。表头包括：username、password、realName、phone、salary、role、remark~'
  },
  {
    keywords: ['导出', '员工', 'excel', '下载'],
    question: '如何导出员工数据？',
    answer: '喵~ 在"员工管理"页面，点击"导出Excel"按钮，系统会自动下载所有员工数据的 Excel 文件~'
  },
  {
    keywords: ['修改密码', '改密码', '密码'],
    question: '如何修改密码？',
    answer: '喵~ 点击左侧"个人信息"，在页面中可以修改你的密码。需要输入旧密码和新密码，确认后保存即可~'
  },

  // ==================== 会员管理 ====================
  {
    keywords: ['添加', '会员', '新增', '注册'],
    question: '如何添加会员？',
    answer: '喵~ 点击左侧"会员管理"，在页面顶部点击"添加会员"按钮，填写会员的姓名、电话、等级（普通/银卡/金卡/钻石）等信息后保存。会员管理功能仅管理员可用哦~'
  },
  {
    keywords: ['编辑', '修改', '会员'],
    question: '如何编辑会员信息？',
    answer: '喵~ 在"会员管理"页面找到目标会员，点击"编辑"按钮进行修改后保存即可~'
  },
  {
    keywords: ['删除', '会员'],
    question: '如何删除会员？',
    answer: '喵~ 在"会员管理"页面，点击对应会员的"删除"按钮，确认后即可删除~'
  },
  {
    keywords: ['会员等级', '等级', '银卡', '金卡', '钻石', '积分'],
    question: '会员等级和积分是怎么算的？',
    answer: '喵~ 会员分为四个等级：普通会员、银卡会员、金卡会员、钻石会员。会员等级根据消费积分自动升级，积分越多等级越高，享受的折扣也越大哦~'
  },
  {
    keywords: ['会员卡', '关联', '会员卡号'],
    question: '如何关联会员卡？',
    answer: '喵~ 添加会员时需要填写会员卡号（cardNumber），这是会员的唯一标识。在收银时输入卡号即可关联会员享受折扣~'
  },
  {
    keywords: ['查询', '搜索', '会员', '查找'],
    question: '如何查找某个会员？',
    answer: '喵~ 在"会员管理"页面顶部有搜索框，可以按姓名、电话、卡号等条件筛选查找会员~'
  },

  // ==================== 商品管理 ====================
  {
    keywords: ['添加', '商品', '新增', '上架'],
    question: '如何添加商品？',
    answer: '喵~ 点击左侧"商品管理"（或"商品信息"），在页面顶部点击"添加商品"按钮，填写商品名称、编码、分类、单价、库存数量、供应商等信息后保存即可~'
  },
  {
    keywords: ['编辑', '修改', '商品', '信息'],
    question: '如何编辑商品信息？',
    answer: '喵~ 在商品列表中找到目标商品，点击"编辑"按钮修改信息后保存~'
  },
  {
    keywords: ['删除', '商品', '下架'],
    question: '如何删除商品？',
    answer: '喵~ 在商品列表中找到对应商品，点击"删除"按钮确认即可。注意：已有采购或销售记录的商品可能无法直接删除哦~'
  },
  {
    keywords: ['库存', '商品库存', '库存数量', '查看库存'],
    question: '如何查看商品库存？',
    answer: '喵~ 在"商品管理"页面的商品列表中，可以直接看到每个商品的库存数量。也可以通过搜索框筛选特定商品查看库存~'
  },
  {
    keywords: ['分类', '商品分类', '筛选'],
    question: '如何按分类筛选商品？',
    answer: '喵~ 在"商品管理"页面顶部的筛选区域，可以选择商品分类进行筛选，系统会只显示该分类下的商品~'
  },
  {
    keywords: ['供应商', '商品', '关联'],
    question: '商品如何关联供应商？',
    answer: '喵~ 添加或编辑商品时，有一个"供应商"下拉框，可以选择系统中已有的供应商进行关联。需要先在"供应商管理"中添加供应商哦~'
  },

  // ==================== 采购管理 ====================
  {
    keywords: ['添加', '采购单', '创建', '采购'],
    question: '如何创建采购单？',
    answer: '喵~ 点击左侧"采购管理"（或"采购信息"），在页面顶部点击"添加采购单"按钮。先填写采购主单信息（供应商、日期等），然后添加采购明细（选择商品、填写数量和单价），保存后采购单就创建好啦~'
  },
  {
    keywords: ['编辑', '修改', '采购单'],
    question: '如何修改采购单？',
    answer: '喵~ 在采购单列表中找到目标采购单，点击"编辑"按钮。在采购单未被审批之前可以修改，审批后就不能改了哦~'
  },
  {
    keywords: ['删除', '采购单'],
    question: '如何删除采购单？',
    answer: '喵~ 在采购单列表中，点击对应采购单的"删除"按钮确认即可。已审批通过的采购单可能无法直接删除~'
  },
  {
    keywords: ['采购退货', '退货单', '退货'],
    question: '如何添加采购退货单？',
    answer: '喵~ 在"采购管理"页面，切换到"采购退货"标签页，点击"添加退货单"按钮。选择要退货的原采购单，填写退货商品和数量，保存后即可创建采购退货单~'
  },
  {
    keywords: ['采购明细', '采购详情', '查看采购'],
    question: '如何查看采购单详情？',
    answer: '喵~ 在采购单列表中点击某个采购单的"查看详情"或"明细"按钮，可以看到该采购单的所有商品明细~'
  },
  {
    keywords: ['审批', '采购审批', '状态'],
    question: '采购单有哪些状态？如何审批？',
    answer: '喵~ 采购单一般有"待审批"、"已通过"、"已拒绝"等状态。一号和三号管理员可以审批采购单，在采购单详情中点击"通过"或"拒绝"按钮即可~'
  },
  {
    keywords: ['查询', '搜索', '采购记录', '采购历史'],
    question: '如何查询历史采购记录？',
    answer: '喵~ 在"采购管理"页面顶部有搜索筛选区域，可以按供应商、日期范围、状态等条件查询采购记录~'
  },
  {
    keywords: ['销售', '销售单', '卖'],
    question: '如何添加销售单？',
    answer: '喵~ 销售功能在系统的收银/前台模块中操作。在收银界面选择商品（扫描条码或搜索），选择会员（如有），确认金额后完成收款即可生成销售单~'
  },

  // ==================== 供应商管理 ====================
  {
    keywords: ['添加', '供应商', '新增'],
    question: '如何添加供应商？',
    answer: '喵~ 点击左侧"供应商管理"，在页面顶部点击"添加供应商"按钮，填写供应商名称、联系人、电话、地址等信息后保存~'
  },
  {
    keywords: ['编辑', '修改', '供应商'],
    question: '如何编辑供应商信息？',
    answer: '喵~ 在供应商列表中找到目标供应商，点击"编辑"按钮修改信息后保存即可~'
  },
  {
    keywords: ['删除', '供应商'],
    question: '如何删除供应商？',
    answer: '喵~ 在供应商列表中找到对应供应商，点击"删除"按钮确认即可。如果该供应商有关联的商品，删除前需要先解除关联哦~'
  },
  {
    keywords: ['查询', '搜索', '供应商', '查找'],
    question: '如何查找供应商？',
    answer: '喵~ 在"供应商管理"页面顶部有搜索框，可以按名称、联系人、电话等条件筛选查找供应商~'
  },
  {
    keywords: ['联系人', '供应商电话', '供应商地址'],
    question: '供应商信息包括哪些？',
    answer: '喵~ 供应商信息主要包括：供应商名称、联系人姓名、联系电话、地址、备注等。添加供应商时建议尽量填写完整，方便后续采购时联系~'
  },

  // ==================== 通用操作 ====================
  {
    keywords: ['退出', '登出', '退出登录', '注销'],
    question: '如何退出登录？',
    answer: '喵~ 点击左侧菜单底部的"退出登录"按钮即可安全退出系统~'
  },
  {
    keywords: ['角色', '普通用户', '管理员', '区别'],
    question: '普通用户和管理员有什么区别？',
    answer: '喵~ 普通用户只能查看自己有权限的模块（只读或完全隐藏）。管理员根据不同等级有不同的管理权限。一号管理员拥有全部权限！'
  },
  {
    keywords: ['导航', '菜单', '侧边栏', '在哪'],
    question: '系统有哪些功能模块？在哪里导航？',
    answer: '喵~ 登录后左侧有导航菜单，包括：个人信息、商品管理、采购管理、供应商管理、员工管理、会员管理等。鼠标悬停在左侧图标上可以看到菜单名称~'
  },
  {
    keywords: ['权限不足', '没有权限', '无法', '不能'],
    question: '操作时提示权限不足怎么办？',
    answer: '喵~ 如果提示权限不足，说明你的账号没有该操作的权限。请联系一号管理员为你分配相应的权限。不同管理员等级有不同的管理范围哦~'
  },
  {
    keywords: ['个人信息', '头像', '个人资料'],
    question: '如何修改个人信息？',
    answer: '喵~ 点击左侧"个人信息"菜单，可以查看和修改你的个人信息，包括姓名、电话、密码等。也可以上传自定义头像~'
  },
  {
    keywords: ['帮助', '使用', '教程', '怎么用'],
    question: '如何使用这个系统？',
    answer: '喵~ 欢迎使用超市进销存管理系统！登录后左侧有功能导航菜单，点击进入各模块即可操作。我是你的智能助手，有任何操作问题都可以问我！比如：\n"如何添加商品？"\n"如何创建采购单？"\n"如何管理员工？"\n直接输入你的问题，我会尽力帮你解答~'
  },
  {
    keywords: ['仓库', '仓库管理'],
    question: '如何管理仓库？',
    answer: '喵~ 仓库管理主要通过商品管理模块来实现。在商品管理中可以查看和管理各商品的库存数量。采购入库后库存自动增加，销售出库后库存自动减少。可以在采购管理中处理采购入库和退货出库的库存变动~'
  }
]

// 不合规问题关键词列表
const OFF_TOPIC_PATTERNS = [
  { keywords: ['天气', '下雨', '晴天', '温度', '气候'], reply: '喵~ 我是进销存系统的小助手，只能回答系统操作相关的问题哦！试试问我"如何添加商品"吧~' },
  { keywords: ['新闻', '政治', '股票', '基金', '财经'], reply: '喵？这个问题超出我的范围啦~ 请问关于系统操作的任何问题，我都会热情解答！' },
  { keywords: ['色情', '暴力', '赌博', '违法'], reply: '喵？这个问题超出我的范围啦~ 请问关于系统操作的任何问题，我都会热情解答！' },
  { keywords: ['骂', '傻', '笨', '蠢', '垃圾', '滚', '废物', '智障'], reply: '喵... 我只想帮你用好进销存系统，有什么操作上的困难可以告诉我~' },
  { keywords: ['你是谁', '你叫什么', '你的名字'], reply: '喵~ 我是进销存系统的AI助手，是一只白色小猫！你可以叫我"小喵"，有什么系统操作问题尽管问我~' },
  { keywords: ['你好', '嗨', 'hello', 'hi'], reply: '喵~ 你好呀！我是进销存系统的小助手，有什么可以帮助你的吗？' },
  { keywords: ['谢谢', '感谢', '多谢', 'thanks'], reply: '喵~ 不客气！能帮到你我很开心，还有其他问题随时问我哦~' }
]

/**
 * 计算两个字符串集合的 Jaccard 相似度
 */
function jaccardSimilarity(setA, setB) {
  if (setA.size === 0 || setB.size === 0) return 0
  let intersection = 0
  for (const item of setA) {
    if (setB.has(item)) intersection++
  }
  const union = setA.size + setB.size - intersection
  return intersection / union
}

/**
 * 从文本中提取关键词（简单分词）
 */
function extractKeywords(text) {
  // 移除标点，按空格和常见分隔符分词
  const cleaned = text.replace(/[，。！？、；：""''【】《》（）\s,.!?;:'"()\[\]{}#@$%^&*+=|\\/~`]+/g, ' ')
  const words = cleaned.split(' ').filter(w => w.length > 0)
  // 同时加入单字做短词匹配（中文字符）
  const chars = text.replace(/[^一-龥]/g, '').split('')
  return new Set([...words, ...chars])
}

/**
 * 检查是否为不合规/无关问题，命中则返回预设回复，否则返回 null
 */
function checkOffTopic(message) {
  if (!message || message.trim().length === 0) {
    return '喵？你什么都没说呢~ 输入你的问题，我会尽力帮你！'
  }
  const lower = message.toLowerCase()
  for (const pattern of OFF_TOPIC_PATTERNS) {
    const matched = pattern.keywords.some(kw => lower.includes(kw))
    if (matched) return pattern.reply
  }
  return null
}

/**
 * 在知识库中匹配最佳回答
 * @param {string} message - 用户输入
 * @returns {{ answer: string, question: string } | null} - 匹配结果，未命中返回 null
 */
export function matchKnowledge(message) {
  // 先检查不合规/无关问题
  const offTopicReply = checkOffTopic(message)
  if (offTopicReply) {
    return { answer: offTopicReply, question: '' }
  }

  const inputKeywords = extractKeywords(message)
  let bestMatch = null
  let bestScore = 0

  for (const item of knowledgeBase) {
    const itemKeywords = new Set([...item.keywords])
    const score = jaccardSimilarity(inputKeywords, itemKeywords)

    if (score > bestScore) {
      bestScore = score
      bestMatch = item
    }
  }

  // 阈值 > 0.3 认为命中
  if (bestMatch && bestScore > 0.3) {
    return { answer: bestMatch.answer, question: bestMatch.question }
  }

  return null
}

/**
 * 获取所有快捷问题（用于对话面板的快捷标签）
 */
export function getQuickQuestions() {
  return [
    '如何添加员工？',
    '如何添加会员？',
    '如何添加商品？',
    '如何创建采购单？',
    '如何添加采购退货单？',
    '如何添加供应商？',
    '管理员等级有什么区别？',
    '如何修改密码？'
  ]
}

export default knowledgeBase
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/utils/knowledgeBase.js
git commit -m "feat: 添加本地知识库 — 40+ 预设问答 + 关键词匹配 + 不合规过滤"
```

---

### Task 7: 创建 CatAvatar 组件（小猫形象）

**Files:**
- Create: `frontend/src/components/cat/CatAvatar.vue`

- [ ] **Step 1: 创建 CatAvatar.vue**

```vue
<template>
  <div
    class="cat-avatar"
    :class="{ 'is-dragging': isDragging, 'is-jumping': isJumping }"
    :style="positionStyle"
    @mousedown="onMouseDown"
    @touchstart.prevent="onTouchStart"
    @dblclick="resetPosition"
  >
    <!-- 视频播放小猫 -->
    <video
      ref="videoRef"
      class="cat-video"
      src="/cat-agent.mp4"
      autoplay
      loop
      muted
      playsinline
      preload="auto"
    ></video>

    <!-- 状态气泡 -->
    <transition name="bubble-fade">
      <div v-if="bubbleText" class="cat-bubble" @click.stop>
        {{ bubbleText }}
      </div>
    </transition>

    <!-- 模式角标 -->
    <span class="cat-badge" :title="modeLabel">{{ modeIcon }}</span>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  mode: { type: String, default: 'smart' },  // smart | quiet | fold
  initialX: { type: Number, default: 0 },
  initialY: { type: Number, default: 0 }
})

const emit = defineEmits(['click', 'positionChange'])

const videoRef = ref(null)
const isDragging = ref(false)
const isJumping = ref(false)
const bubbleText = ref('')
const posX = ref(0)
const posY = ref(0)

const catSize = computed(() => props.mode === 'fold' ? 40 : 80)

const positionStyle = computed(() => ({
  left: posX.value + 'px',
  top: posY.value + 'px',
  width: catSize.value + 'px',
  height: catSize.value + 'px'
}))

const modeIcon = computed(() => {
  switch (props.mode) {
    case 'smart': return '🐾'
    case 'quiet': return '🌙'
    case 'fold': return '📌'
    default: return '🐾'
  }
})

const modeLabel = computed(() => {
  switch (props.mode) {
    case 'smart': return '灵动模式'
    case 'quiet': return '安静模式'
    case 'fold': return '折叠模式'
    default: return ''
  }
})

// 初始化位置
onMounted(() => {
  const savedX = localStorage.getItem('catPosX')
  const savedY = localStorage.getItem('catPosY')
  if (savedX !== null && savedY !== null) {
    posX.value = parseInt(savedX)
    posY.value = parseInt(savedY)
  } else {
    posX.value = window.innerWidth - 100
    posY.value = window.innerHeight - 200
  }

  // 呼吸动画
  startBreathing()

  // 灵动模式：定时主动提示 + 跳跃
  if (props.mode === 'smart') {
    startSmartBehavior()
  }
})

// 保存位置
function savePosition() {
  localStorage.setItem('catPosX', posX.value)
  localStorage.setItem('catPosY', posY.value)
}

// 重置到默认位置
function resetPosition() {
  posX.value = window.innerWidth - 100
  posY.value = window.innerHeight - 200
  savePosition()
}

// 呼吸动画（周期性缩放）
let breathTimer = null
function startBreathing() {
  // CSS handles breathing via animation
}

// 灵动模式主动行为
let smartTimer = null
function startSmartBehavior() {
  smartTimer = setInterval(() => {
    // 随机跳跃
    if (Math.random() > 0.6) {
      isJumping.value = true
      setTimeout(() => { isJumping.value = false }, 600)
    }
  }, 15000) // 每 15 秒可能跳一下
}

// ========== 拖拽 ==========
let dragStartX = 0
let dragStartY = 0
let startPosX = 0
let startPosY = 0
let hasMoved = false

function clampPosition(x, y) {
  const maxX = window.innerWidth - catSize.value - 10
  const maxY = window.innerHeight - catSize.value - 10
  return {
    x: Math.max(10, Math.min(x, maxX)),
    y: Math.max(10, Math.min(y, maxY))
  }
}

function onMouseDown(e) {
  if (e.button !== 0) return
  dragStartX = e.clientX
  dragStartY = e.clientY
  startPosX = posX.value
  startPosY = posY.value
  hasMoved = false
  isDragging.value = true
  document.addEventListener('mousemove', onMouseMove)
  document.addEventListener('mouseup', onMouseUp)
}

function onMouseMove(e) {
  const dx = e.clientX - dragStartX
  const dy = e.clientY - dragStartY
  if (Math.abs(dx) > 3 || Math.abs(dy) > 3) hasMoved = true
  const clamped = clampPosition(startPosX + dx, startPosY + dy)
  posX.value = clamped.x
  posY.value = clamped.y
}

function onMouseUp() {
  isDragging.value = false
  document.removeEventListener('mousemove', onMouseMove)
  document.removeEventListener('mouseup', onMouseUp)
  savePosition()
  if (!hasMoved) {
    emit('click')
  }
}

function onTouchStart(e) {
  const touch = e.touches[0]
  dragStartX = touch.clientX
  dragStartY = touch.clientY
  startPosX = posX.value
  startPosY = posY.value
  hasMoved = false
  isDragging.value = true
  document.addEventListener('touchmove', onTouchMove, { passive: false })
  document.addEventListener('touchend', onTouchEnd)
}

function onTouchMove(e) {
  e.preventDefault()
  const touch = e.touches[0]
  const dx = touch.clientX - dragStartX
  const dy = touch.clientY - dragStartY
  if (Math.abs(dx) > 5 || Math.abs(dy) > 5) hasMoved = true
  const clamped = clampPosition(startPosX + dx, startPosY + dy)
  posX.value = clamped.x
  posY.value = clamped.y
}

function onTouchEnd() {
  isDragging.value = false
  document.removeEventListener('touchmove', onTouchMove)
  document.removeEventListener('touchend', onTouchEnd)
  savePosition()
  if (!hasMoved) {
    emit('click')
  }
}

// 暴露方法给父组件
function showBubble(text, duration = 5000) {
  bubbleText.value = text
  if (duration > 0) {
    setTimeout(() => { bubbleText.value = '' }, duration)
  }
}

defineExpose({ showBubble, resetPosition })

onUnmounted(() => {
  if (smartTimer) clearInterval(smartTimer)
  if (breathTimer) clearInterval(breathTimer)
})
</script>

<style scoped>
.cat-avatar {
  position: fixed;
  z-index: 9999;
  cursor: grab;
  user-select: none;
  border-radius: 50%;
  overflow: visible;
  transition: width 0.3s ease, height 0.3s ease;
}

.cat-avatar.is-dragging {
  cursor: grabbing;
  transition: none;
}

.cat-avatar.is-jumping {
  animation: catJump 0.6s ease;
}

@keyframes catJump {
  0%, 100% { transform: translateY(0); }
  30% { transform: translateY(-18px) scale(1.08); }
  50% { transform: translateY(-22px) scale(1.05); }
  70% { transform: translateY(-8px) scale(0.95); }
}

/* 视频播放 */
.cat-video {
  width: 100%;
  height: 100%;
  border-radius: 50%;
  object-fit: cover;
  /* 柔边遮罩融合白底 */
  box-shadow:
    0 0 0 6px rgba(255, 255, 255, 0.75),
    0 0 20px 8px rgba(255, 240, 220, 0.5),
    0 4px 16px rgba(180, 130, 80, 0.15);
  animation: catBreathe 4s ease-in-out infinite;
}

@keyframes catBreathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

/* 状态气泡 */
.cat-bubble {
  position: absolute;
  bottom: calc(100% + 10px);
  left: 50%;
  transform: translateX(-50%);
  background: rgba(255, 255, 255, 0.95);
  color: #5a3e28;
  padding: 8px 14px;
  border-radius: 16px;
  font-size: 13px;
  white-space: nowrap;
  box-shadow: 0 2px 12px rgba(0,0,0,0.1);
  pointer-events: none;
}

.bubble-fade-enter-active, .bubble-fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.bubble-fade-enter-from, .bubble-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(6px);
}

/* 模式角标 */
.cat-badge {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 22px;
  height: 22px;
  background: rgba(255, 255, 255, 0.95);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  box-shadow: 0 1px 4px rgba(0,0,0,0.15);
  pointer-events: none;
}

/* 折叠模式 */
.cat-avatar:has(.cat-badge:contains('📌')) .cat-video {
  box-shadow:
    0 0 0 3px rgba(255, 255, 255, 0.75),
    0 2px 8px rgba(180, 130, 80, 0.12);
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/cat/CatAvatar.vue
git commit -m "feat: 添加 CatAvatar 组件 — 小猫形象 + 拖拽 + 状态动画"
```

---

### Task 8: 创建 ChatDialog 组件（对话面板）

**Files:**
- Create: `frontend/src/components/cat/ChatDialog.vue`

- [ ] **Step 1: 创建 ChatDialog.vue**

```vue
<template>
  <transition name="dialog-slide">
    <div
      v-if="visible"
      class="chat-dialog"
      :style="dialogStyle"
    >
      <!-- 头部 -->
      <div class="chat-header">
        <span class="chat-title">🐱 小喵助手</span>
        <div class="chat-header-actions">
          <button class="chat-btn-icon" title="设置" @click="$emit('openSettings')">⚙️</button>
          <button class="chat-btn-icon" title="关闭" @click="$emit('close')">✕</button>
        </div>
      </div>

      <!-- 消息列表 -->
      <div class="chat-messages" ref="messagesRef">
        <div v-if="messages.length === 0" class="chat-empty">
          <div class="chat-empty-icon">🐱</div>
          <p>喵~ 我是进销存系统小助手</p>
          <p class="chat-empty-hint">点击下方快捷问题或输入你的问题</p>
        </div>

        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          class="chat-msg"
          :class="msg.role === 'user' ? 'msg-user' : 'msg-agent'"
        >
          <div class="msg-bubble">{{ msg.content }}</div>
        </div>

        <!-- 正在输入中 -->
        <div v-if="isLoading" class="chat-msg msg-agent">
          <div class="msg-bubble typing-bubble">
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
            <span class="typing-dot"></span>
          </div>
        </div>
      </div>

      <!-- 快捷问题 -->
      <div class="chat-quick-questions">
        <span
          v-for="(q, idx) in quickQuestions"
          :key="idx"
          class="quick-tag"
          @click="$emit('send', q)"
        >{{ q }}</span>
      </div>

      <!-- 输入区 -->
      <div class="chat-input-area">
        <input
          ref="inputRef"
          v-model="inputText"
          class="chat-input"
          placeholder="输入你的问题..."
          @keyup.enter="sendMessage"
        />
        <button class="chat-send-btn" @click="sendMessage" :disabled="!inputText.trim() || isLoading">
          发送
        </button>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import { getQuickQuestions } from '@/utils/knowledgeBase'

const props = defineProps({
  visible: { type: Boolean, default: false },
  messages: { type: Array, default: () => [] },
  isLoading: { type: Boolean, default: false },
  anchorX: { type: Number, default: 0 },
  anchorY: { type: Number, default: 0 }
})

const emit = defineEmits(['send', 'close', 'openSettings'])

const inputRef = ref(null)
const messagesRef = ref(null)
const inputText = ref('')
const quickQuestions = ref(getQuickQuestions())

// 对话面板定位（小猫附近）
const dialogStyle = computed(() => {
  const panelW = 360
  const panelH = 480
  let left = props.anchorX - panelW + 80  // 默认右边对齐小猫
  let top = props.anchorY - panelH - 10

  // 边界检查
  if (left < 10) left = 10
  if (left + panelW > window.innerWidth - 10) left = window.innerWidth - panelW - 10
  if (top < 10) top = props.anchorY + 90
  if (top + panelH > window.innerHeight - 10) top = window.innerHeight - panelH - 10

  return {
    left: left + 'px',
    top: top + 'px'
  }
})

// 当面板打开时聚焦输入框
watch(() => props.visible, async (val) => {
  if (val) {
    await nextTick()
    inputRef.value?.focus()
  }
})

// 新消息到达时滚动到底部
watch(() => props.messages.length, async () => {
  await nextTick()
  if (messagesRef.value) {
    messagesRef.value.scrollTop = messagesRef.value.scrollHeight
  }
})

function sendMessage() {
  const text = inputText.value.trim()
  if (!text || props.isLoading) return
  emit('send', text)
  inputText.value = ''
}
</script>

<style scoped>
.chat-dialog {
  position: fixed;
  z-index: 10000;
  width: 360px;
  height: 480px;
  background: rgba(255, 255, 255, 0.97);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-radius: 16px;
  box-shadow: 0 8px 36px rgba(120, 80, 30, 0.18), 0 0 0 1px rgba(180, 130, 80, 0.1);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.dialog-slide-enter-active, .dialog-slide-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}
.dialog-slide-enter-from, .dialog-slide-leave-to {
  opacity: 0;
  transform: translateY(12px) scale(0.96);
}

/* 头部 */
.chat-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(200, 150, 100, 0.15);
  flex-shrink: 0;
}
.chat-title { font-weight: 600; color: #5a3e28; font-size: 15px; }
.chat-header-actions { display: flex; gap: 4px; }
.chat-btn-icon {
  width: 28px; height: 28px; border: none; background: transparent;
  cursor: pointer; border-radius: 6px; font-size: 14px; color: #9b8570;
  display: flex; align-items: center; justify-content: center;
}
.chat-btn-icon:hover { background: rgba(200, 150, 100, 0.1); color: #5a3e28; }

/* 消息列表 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px 14px;
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.chat-messages::-webkit-scrollbar { width: 4px; }
.chat-messages::-webkit-scrollbar-thumb { background: rgba(180, 130, 80, 0.15); border-radius: 4px; }

.chat-empty { text-align: center; padding: 40px 20px; color: #9b8570; }
.chat-empty-icon { font-size: 40px; margin-bottom: 10px; }
.chat-empty-hint { font-size: 12px; margin-top: 6px; }

.chat-msg { display: flex; }
.msg-user { justify-content: flex-end; }
.msg-agent { justify-content: flex-start; }

.msg-bubble {
  max-width: 80%;
  padding: 10px 14px;
  border-radius: 14px;
  font-size: 13px;
  line-height: 1.6;
  word-break: break-word;
  white-space: pre-wrap;
}
.msg-user .msg-bubble {
  background: linear-gradient(135deg, #8b6fd4, #a78bfa);
  color: #fff;
  border-bottom-right-radius: 4px;
}
.msg-agent .msg-bubble {
  background: #fef9f0;
  color: #5a3e28;
  border: 1px solid rgba(200, 150, 100, 0.12);
  border-bottom-left-radius: 4px;
}

/* 输入中动画 */
.typing-bubble {
  display: flex; gap: 4px; padding: 14px 16px;
}
.typing-dot {
  width: 6px; height: 6px; border-radius: 50%;
  background: rgba(180, 130, 80, 0.4);
  animation: typingBounce 1.2s ease-in-out infinite;
}
.typing-dot:nth-child(2) { animation-delay: 0.15s; }
.typing-dot:nth-child(3) { animation-delay: 0.3s; }
@keyframes typingBounce {
  0%, 60%, 100% { transform: translateY(0); }
  30% { transform: translateY(-6px); }
}

/* 快捷问题 */
.chat-quick-questions {
  display: flex;
  gap: 6px;
  padding: 8px 14px;
  overflow-x: auto;
  flex-shrink: 0;
  border-top: 1px solid rgba(200, 150, 100, 0.08);
}
.chat-quick-questions::-webkit-scrollbar { height: 0; }
.quick-tag {
  flex-shrink: 0;
  padding: 4px 12px;
  background: #fef5ea;
  color: #b8661e;
  border-radius: 12px;
  font-size: 12px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s;
  border: 1px solid rgba(220, 160, 90, 0.15);
}
.quick-tag:hover { background: #fde8cf; }

/* 输入区 */
.chat-input-area {
  display: flex;
  gap: 8px;
  padding: 10px 14px;
  border-top: 1px solid rgba(200, 150, 100, 0.12);
  flex-shrink: 0;
}
.chat-input {
  flex: 1;
  border: 1px solid rgba(200, 150, 100, 0.2);
  border-radius: 20px;
  padding: 8px 14px;
  font-size: 13px;
  outline: none;
  color: #5a3e28;
  background: #fefcf8;
  transition: border-color 0.2s;
}
.chat-input:focus { border-color: #d4843b; }
.chat-send-btn {
  border: none;
  padding: 8px 18px;
  border-radius: 20px;
  background: linear-gradient(135deg, #d4843b, #e8a85f);
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  white-space: nowrap;
  transition: opacity 0.2s;
}
.chat-send-btn:hover { opacity: 0.9; }
.chat-send-btn:disabled { opacity: 0.5; cursor: not-allowed; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/cat/ChatDialog.vue
git commit -m "feat: 添加 ChatDialog 组件 — 对话面板 + 快捷问题 + 输入中动画"
```

---

### Task 9: 创建 SettingsPanel 组件（设置面板）

**Files:**
- Create: `frontend/src/components/cat/SettingsPanel.vue`

- [ ] **Step 1: 创建 SettingsPanel.vue**

```vue
<template>
  <transition name="settings-fade">
    <div v-if="visible" class="settings-overlay" @click.self="$emit('close')">
      <div class="settings-panel">
        <div class="settings-header">
          <span>🐱 小喵设置</span>
          <button class="settings-close" @click="$emit('close')">✕</button>
        </div>

        <div class="settings-body">
          <div class="settings-section">
            <div class="settings-label">交互模式</div>
            <div class="mode-options">
              <div
                v-for="opt in modeOptions"
                :key="opt.value"
                class="mode-card"
                :class="{ active: currentMode === opt.value }"
                @click="$emit('update:mode', opt.value)"
              >
                <span class="mode-emoji">{{ opt.emoji }}</span>
                <div class="mode-info">
                  <div class="mode-name">{{ opt.name }}</div>
                  <div class="mode-desc">{{ opt.desc }}</div>
                </div>
              </div>
            </div>
          </div>

          <div class="settings-section">
            <div class="settings-label">关于小喵</div>
            <p class="settings-about">
              小喵是进销存管理系统的 AI 助手，采用"本地知识库 + DeepSeek AI"混合方案。
              预设 40+ 条常见操作问答，覆盖员工管理、会员管理、商品管理、采购管理、供应商管理等模块。
            </p>
          </div>
        </div>
      </div>
    </div>
  </transition>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  visible: { type: Boolean, default: false },
  mode: { type: String, default: 'smart' }
})

const emit = defineEmits(['update:mode', 'close'])

const currentMode = computed(() => props.mode)

const modeOptions = [
  { value: 'smart', emoji: '🐾', name: '灵动模式', desc: '主动发现你的困难，偶尔跳跃撒娇' },
  { value: 'quiet', emoji: '🌙', name: '安静模式', desc: '安静陪伴，点击才打开对话' },
  { value: 'fold', emoji: '📌', name: '折叠模式', desc: '缩小为图标，最小化存在感' }
]
</script>

<style scoped>
.settings-overlay {
  position: fixed; inset: 0; z-index: 10001;
  background: rgba(0,0,0,0.3);
  display: flex; align-items: center; justify-content: center;
}
.settings-panel {
  width: 360px; max-height: 80vh;
  background: #fff; border-radius: 16px;
  box-shadow: 0 12px 40px rgba(0,0,0,0.2);
  overflow: hidden;
}
.settings-header {
  display: flex; align-items: center; justify-content: space-between;
  padding: 14px 18px; border-bottom: 1px solid #f0e6d8;
  font-weight: 600; color: #5a3e28;
}
.settings-close {
  border: none; background: transparent; cursor: pointer;
  font-size: 16px; color: #9b8570;
}
.settings-body { padding: 16px 18px; }
.settings-section { margin-bottom: 20px; }
.settings-label { font-size: 14px; font-weight: 600; color: #5a3e28; margin-bottom: 10px; }

.mode-options { display: flex; flex-direction: column; gap: 8px; }
.mode-card {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 12px;
  cursor: pointer; border: 2px solid transparent;
  transition: all 0.2s;
  background: #fefcf8;
}
.mode-card:hover { background: #fef5ea; }
.mode-card.active { border-color: #d4843b; background: #fef5ea; }
.mode-emoji { font-size: 28px; flex-shrink: 0; }
.mode-name { font-weight: 600; color: #5a3e28; font-size: 14px; }
.mode-desc { font-size: 12px; color: #9b8570; margin-top: 2px; }

.settings-about { font-size: 13px; color: #6b4d34; line-height: 1.8; }

.settings-fade-enter-active, .settings-fade-leave-active { transition: opacity 0.2s; }
.settings-fade-enter-from, .settings-fade-leave-to { opacity: 0; }
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/cat/SettingsPanel.vue
git commit -m "feat: 添加 SettingsPanel 组件 — 模式切换设置面板"
```

---

### Task 10: 创建 CatAgent 总控组件

**Files:**
- Create: `frontend/src/components/cat/CatAgent.vue`

- [ ] **Step 1: 创建 CatAgent.vue**

```vue
<template>
  <div class="cat-agent-root">
    <!-- 小猫形象 -->
    <CatAvatar
      ref="avatarRef"
      :mode="mode"
      @click="toggleChat"
    />

    <!-- 对话面板 -->
    <ChatDialog
      :visible="chatVisible"
      :messages="messages"
      :isLoading="isLoading"
      :anchorX="avatarPos.x"
      :anchorY="avatarPos.y"
      @send="handleSend"
      @close="chatVisible = false"
      @openSettings="settingsVisible = true"
    />

    <!-- 设置面板 -->
    <SettingsPanel
      :visible="settingsVisible"
      :mode="mode"
      @update:mode="setMode"
      @close="settingsVisible = false"
    />
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import CatAvatar from './CatAvatar.vue'
import ChatDialog from './ChatDialog.vue'
import SettingsPanel from './SettingsPanel.vue'
import { matchKnowledge } from '@/utils/knowledgeBase'
import { sendMessage } from '@/api/agent'

const mode = ref('smart')
const chatVisible = ref(false)
const settingsVisible = ref(false)
const isLoading = ref(false)
const messages = ref([])  // [{role: 'user'|'agent', content: '...'}]

const avatarPos = reactive({ x: 0, y: 0 })

const avatarRef = ref(null)

// 初始化：读取保存的模式 + 监听小猫位置
onMounted(() => {
  const savedMode = localStorage.getItem('catMode')
  if (savedMode && ['smart', 'quiet', 'fold'].includes(savedMode)) {
    mode.value = savedMode
  }

  const savedX = localStorage.getItem('catPosX')
  const savedY = localStorage.getItem('catPosY')
  if (savedX !== null && savedY !== null) {
    avatarPos.x = parseInt(savedX)
    avatarPos.y = parseInt(savedY)
  } else {
    avatarPos.x = window.innerWidth - 100
    avatarPos.y = window.innerHeight - 200
  }

  // 定时同步小猫位置
  const syncPos = setInterval(() => {
    const x = localStorage.getItem('catPosX')
    const y = localStorage.getItem('catPosY')
    if (x) avatarPos.x = parseInt(x)
    if (y) avatarPos.y = parseInt(y)
  }, 500)

  onUnmounted(() => clearInterval(syncPos))

  // 灵动模式：30s 后主动提示
  if (mode.value === 'smart') {
    setTimeout(() => {
      if (!chatVisible.value && mode.value === 'smart') {
        avatarRef.value?.showBubble('需要帮助吗？点我~', 8000)
      }
    }, 30000)
  }
})

// 清理：限制消息数量
function cleanMessages() {
  if (messages.value.length > 50) {
    messages.value = messages.value.slice(-30)
  }
}

function toggleChat() {
  chatVisible.value = !chatVisible.value
}

function setMode(newMode) {
  mode.value = newMode
  localStorage.setItem('catMode', newMode)
}

async function handleSend(text) {
  // 添加用户消息
  messages.value.push({ role: 'user', content: text })
  cleanMessages()

  // 1. 先尝试本地知识库匹配
  const localMatch = matchKnowledge(text)
  if (localMatch) {
    messages.value.push({ role: 'agent', content: localMatch.answer })
    cleanMessages()
    return
  }

  // 2. 未命中，调用后端 DeepSeek API
  isLoading.value = true
  try {
    const history = messages.value.slice(0, -1).map(m => ({
      role: m.role === 'agent' ? 'assistant' : m.role,
      content: m.content
    }))
    const res = await sendMessage(text, history)
    const reply = res?.data?.reply || '喵~ 抱歉，我暂时无法回答这个问题，请稍后再试~'
    messages.value.push({ role: 'agent', content: reply })
    cleanMessages()
  } catch (e) {
    messages.value.push({ role: 'agent', content: '喵~ 网络好像不太稳定，请检查后端服务是否启动，稍后再试~' })
  } finally {
    isLoading.value = false
  }
}
</script>

<style scoped>
.cat-agent-root {
  /* 无样式，纯容器 */
}
</style>
```

- [ ] **Step 2: 提交**

```bash
git add frontend/src/components/cat/CatAgent.vue
git commit -m "feat: 添加 CatAgent 总控组件 — 模式管理 + 知识库匹配 + API 兜底"
```

---

### Task 11: 在 Layout.vue 中引入 CatAgent

**Files:**
- Modify: `frontend/src/views/Layout.vue`

- [ ] **Step 1: 在 `<script setup>` 中追加 import 语句**

在 `frontend/src/views/Layout.vue` 的 `<script setup>` 中，在现有 import 之后追加：

```js
import CatAgent from '@/components/cat/CatAgent.vue'
```

- [ ] **Step 2: 在 `<template>` 中追加 CatAgent 标签**

在 `<el-container>` 最外层结束后（即 `</el-container>` 之前），追加：

```html
<!-- 智能体：白色小猫助手 -->
<CatAgent />
```

具体位置：在 `</el-container>`（第 111 行的那个）前面插入即可。

- [ ] **Step 3: 提交**

```bash
git add frontend/src/views/Layout.vue
git commit -m "feat: Layout 引入 CatAgent 智能体组件"
```

---

## 验证测试

### 后端验证
```bash
# 启动后端后，用 curl 测试
curl -X POST http://localhost:8080/api/agent/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"如何添加商品？","history":[]}'
# 预期：返回 {"code":200,"data":{"reply":"喵~ ..."}}
```

### 前端验证
1. `npm run dev` 启动前端
2. 登录系统（admin / 123456）
3. 右下角应出现白色小猫
4. 点击小猫 → 弹出对话面板
5. 输入"如何添加商品" → 本地知识库即时返回
6. 输入其他问题 → 转发 DeepSeek 返回
7. 拖拽小猫 → 可移动，位置持久化
8. 点击设置 → 切换安静/折叠模式
9. 退出登录 → 小猫消失
