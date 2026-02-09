package com.example;

import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(properties = {
        "spring.ai.enabled=false",
        "spring.autoconfigure.exclude=org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration,org.springframework.ai.model.tool.autoconfigure.ToolCallingAutoConfiguration"
})
class RecipeFinderClientApplicationTests {

    @Test
    void contextLoads() {}

    @TestConfiguration
    static class TestConfig {
        @Bean ToolCallbackProvider toolCallbackProvider() { return mock(ToolCallbackProvider.class); }
        @Bean ChatModel chatModel() { return mock(ChatModel.class); }
        @Bean ChatClient.Builder chatClientBuilder() {
            var builder = mock(ChatClient.Builder.class, Answers.RETURNS_DEEP_STUBS);
            var chatClient = mock(ChatClient.class, Answers.RETURNS_DEEP_STUBS);
            when(builder.defaultSystem(any(Resource.class))).thenReturn(builder);
            when(builder.build()).thenReturn(chatClient);
            return builder;
        }
    }
}
