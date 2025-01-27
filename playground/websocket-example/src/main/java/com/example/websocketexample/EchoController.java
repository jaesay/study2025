package com.example.websocketexample;

import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
@Slf4j
public class EchoController {

    @MessageMapping("/hello")
    @SendToUser("/queue/messages") // 송신자 본인에게 메시지 전송
    public String sendMessage(EchoMessage message, SimpMessageHeaderAccessor headerAccessor) {
        // WebSocket 세션 ID로 사용자 식별
        String sessionId = headerAccessor.getSessionId();
        log.debug("Session ID: {}, Message: {}", sessionId, message.content());

        // 에코 메시지 전송
        return "Echo from server: " + message.content();
    }
}
