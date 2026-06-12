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
