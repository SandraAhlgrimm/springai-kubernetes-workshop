package com.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
class RecipeFinderConfiguration {

    @Value("classpath:/prompts/fix-json-response")
    private Resource fixJsonResponsePromptResource;

    @Bean
    ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
        return chatClientBuilder.defaultSystem(fixJsonResponsePromptResource).build();
    }

}
