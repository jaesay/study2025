package com.example.sseexample;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

@RestController
@RequestMapping("/sse")
public class SseController {

    private final Map<String, Sinks.Many<String>> userSinks = new ConcurrentHashMap<>();

    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> subscribe(@RequestParam String userId) {
        Sinks.Many<String> sink = Sinks.many().unicast().onBackpressureBuffer();
        userSinks.put(userId, sink);

        // SSE 연결 종료 시, userId 제거 (Flux가 완료되면 자동으로 실행됨)
        return sink.asFlux()
            .timeout(Duration.ofSeconds(30)) // 30초 후 자동 종료
            .doFinally(signalType -> userSinks.remove(userId));
    }

    @PostMapping("/send")
    public void sendEvent(@RequestParam String userId, @RequestParam String message) {
        Sinks.Many<String> sink = userSinks.get(userId);
        if (sink != null) {
            sink.tryEmitNext("Message to " + userId + ": " + message);
            sink.tryEmitComplete(); // 메시지 전송 후 SSE 연결 종료
            userSinks.remove(userId); // Map에서 userId 제거
        }
    }
}
