package com.jaesay.openaidemo.text;

import com.jaesay.openaidemo.services.OpenAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class AnswerAnyThingStreamingController {

	private final OpenAiService service;

    @GetMapping("/stream")
    public Flux<String> answerAnything(@RequestParam("message") String message) {
        return service.streamAnswer(message);
    }
}