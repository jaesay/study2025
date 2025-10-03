package com.jaesay.openaidemo.text;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
@Slf4j
public class AnswerAnyThingController {

    private final OpenAiService chatService;

    @GetMapping("/showAskAnything")
    public String showAskAnything() {
         return "askAnything";
    }

    @PostMapping("/askAnything")
    public String askAnything(@RequestParam("question") String question, Model model) {
        ChatResponse chatResponse = chatService.generateAnswer(question);
        log.info(chatResponse.toString());
        model.addAttribute("question", question);
        model.addAttribute("answer", chatResponse.getResult().getOutput().getText());
        return "askAnything";
    }
}