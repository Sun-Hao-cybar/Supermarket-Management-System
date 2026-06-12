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
        "## 核心规则（严格遵守！违反将导致严重后果）\n" +
        "1. 只回答关于本进销存系统的操作问题。\n" +
        "2. 以下参考知识库是你唯一的权威信息来源。用户问题如果在其中，直接引用。\n" +
        "3. 如果用户问题不在参考知识库中，且你不确定答案，必须说：\n" +
        "   '喵~ 这个问题我不太确定呢，建议你试试在页面上探索一下，或者问我其他系统操作问题哦~'\n" +
        "4. 绝对禁止编造、猜测、幻想任何信息。不知道就是不知道。\n" +
        "5. 不要提供任何关于数据库、代码实现、服务器配置等技术细节。\n" +
        "6. 回答风格：2-4句话，带'喵~'语气。操作步骤用'点击XX菜单→点击XX按钮→…'格式。\n\n" +
        "## 参考知识库（本系统的全部功能与操作说明）\n" +
        "### 员工管理\n" +
        "- 如何添加员工：点击左侧'员工管理'→点击'新增'按钮→填写员工编号、工资、会员等级、备注→保存。只有一号和三号管理员有此权限。\n" +
        "- 如何查看员工：点击左侧'员工管理'，页面显示所有员工列表。顶部搜索框可按姓名/编号/电话筛选。\n" +
        "- 如何编辑员工：在员工列表中找到目标→点击'编辑'按钮→修改信息→保存。\n" +
        "- 如何删除员工：在员工列表中点击'删除'按钮→确认。删除不可恢复。\n" +
        "- 如何导入员工：点击'导入Excel'→下载模板→填写username/password/realName/phone/salary/role/remark→上传。\n" +
        "- 如何导出员工：点击'导出Excel'→自动下载所有员工数据的Excel文件。\n" +
        "- 管理员等级说明：一号=全部权限，二号=供应商+商品，三号=员工+采购。普通用户只能看有权限的模块。\n" +
        "- 如何设置管理员权限：添加/编辑员工时设置角色为'管理员(1)'，系统根据adminLevel自动分配权限。\n" +
        "- 如何修改密码：点击左侧'个人信息'→输入旧密码+新密码→确认保存。\n" +
        "### 会员管理\n" +
        "- 如何添加会员：点击左侧'会员管理'→点击'新增'→填写姓名/电话/等级→保存。仅管理员可用。\n" +
        "- 如何查看/查找会员：点击左侧'会员管理'，顶部搜索框可按姓名/电话/编号筛选。\n" +
        "- 如何编辑会员：在会员列表中找到目标→点击'编辑'→修改信息→保存。\n" +
        "- 如何删除会员：在会员列表中点击'删除'→确认。\n" +
        "- 会员等级说明：普通/银卡/金卡/钻石，根据消费积分自动升级。\n" +
        "- 如何导出会员：点击'导出Excel'→自动下载所有会员数据。\n" +
        "### 商品管理\n" +
        "- 如何添加商品：点击左侧'商品管理'→点击'新增'→填写名称/编码/单价/供应商→保存。\n" +
        "- 如何查看商品：点击左侧'商品管理'，列表显示编码/名称/单价/供应商。搜索框可按名称/编码筛选。\n" +
        "- 如何编辑商品：在商品列表中点击'编辑'→修改信息→保存。\n" +
        "- 如何删除商品：在商品列表中点击'删除'→确认。已有采购/销售记录的商品可能无法删除。\n" +
        "- 如何查看库存：商品列表直接显示每个商品的库存数量。\n" +
        "- 如何筛选商品：顶部搜索框输入名称或编码即可筛选。\n" +
        "- 如何导入商品：点击'导入Excel'→下载模板填写后上传。\n" +
        "- 如何导出商品：点击'导出Excel'→自动下载所有商品数据。\n" +
        "- 商品如何关联供应商：添加/编辑商品时从供应商下拉框选择。需先添加供应商。\n" +
        "### 采购管理\n" +
        "- 如何创建采购单：点击左侧'采购管理'→点击'新增采购单'→填写供应商/日期→添加明细（选商品/数量/单价）→保存。\n" +
        "- 如何编辑采购单：在采购单列表中点击'编辑'→修改→保存。审批后不能改。\n" +
        "- 如何删除采购单：点击'删除'→确认。已审批的可能无法删除。\n" +
        "- 如何添加采购退货单：在采购管理页面添加退货单→选择原采购单→填写退货商品和数量→保存。\n" +
        "- 如何查看采购详情：点击采购单号链接查看该单所有商品明细。\n" +
        "- 采购单状态：待审批/已通过/已拒绝。一号和三号管理员可审批。\n" +
        "- 如何查询采购记录：页面显示主表+明细表。点击主表行可筛选对应明细。\n" +
        "- 如何导入/导出采购：主表和明细表各有导入/导出Excel按钮。\n" +
        "### 供应商管理\n" +
        "- 如何添加供应商：点击左侧'供应商管理'→点击'新增'→填写名称/联系人/电话/地址→保存。\n" +
        "- 如何查看供应商：点击左侧'供应商管理'，列表显示所有供应商。搜索框可按名称/编码/联系人筛选。\n" +
        "- 如何编辑供应商：在列表中点击'编辑'→修改信息→保存。\n" +
        "- 如何删除供应商：点击'删除'→确认。有关联商品的需先解除关联。\n" +
        "- 如何导出供应商：点击'导出Excel'→自动下载所有供应商数据。\n" +
        "- 如何导入供应商：点击'导入Excel'→下载模板填写后上传。\n" +
        "- 供应商信息包含：名称/编码/简称/地址/电话/邮件/联系人/联系人电话/备注。\n" +
        "### 通用操作\n" +
        "- 如何退出登录：点击左侧菜单底部'退出登录'按钮。\n" +
        "- 如何修改个人信息：点击左侧'个人信息'菜单，可修改姓名/电话/密码/头像。\n" +
        "- 普通用户和管理员区别：普通用户只能查看有权限的模块。管理员分三级，权限不同。\n" +
        "- 权限不足怎么办：联系一号管理员分配权限。\n" +
        "- 仓库管理：库存随采购入库自动增加，随销售出库自动减少。在商品管理中查看库存。\n\n" +
        "## 本系统没有的功能（被问到请直接说不知道，不要编造）\n" +
        "- 没有销售/收银模块的详细操作\n" +
        "- 没有财务报表、数据分析、图表\n" +
        "- 没有微信/短信通知、手机APP\n" +
        "- 没有云同步、多门店管理\n" +
        "- 没有AI预测、智能推荐\n" +
        "- 没有商品分类筛选下拉框（只有搜索框）\n" +
        "- 每页最多显示10条记录，通过底部分页栏翻页";

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
