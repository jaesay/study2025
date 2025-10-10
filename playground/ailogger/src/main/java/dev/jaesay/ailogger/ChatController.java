package dev.jaesay.ailogger;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

  private final ChatClient chatClient;
  private final SimpleLoggerAdvisor simpleLoggerAdvisor;

  public ChatController(ChatClient.Builder builder, SimpleLoggerAdvisor simpleLoggerAdvisor) {
    this.chatClient = builder.build();
    this.simpleLoggerAdvisor = simpleLoggerAdvisor;
  }

  @GetMapping
  public ChatResponse index() {
    return chatClient.prompt()
      .user("Tell me an interesting fact about Java.")
      .advisors(simpleLoggerAdvisor)
      .call()
      .chatResponse();
  }

}
