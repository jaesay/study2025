package com.jaesay.openaidemo.moderations;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.moderation.ModerationResult;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class ModerationController {

    private final OpenAiService chatService;

    @GetMapping("/showModeration")
    public String showChatPage() {
         return "moderation";
    }

    @PostMapping("/moderation")
    public String getChatResponse(@RequestParam("text") String text, Model model) {
        ModerationResult moderationResult = chatService.moderate(text);
        model.addAttribute("response", moderationResult);
        return "moderation";
    }
}