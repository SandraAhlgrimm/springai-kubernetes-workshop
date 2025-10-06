package com.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;

@SpringBootApplication
public class RecipeFinderApplication {

  @Value("classpath:/prompts/fix-json-response")
    private Resource fixJsonResponsePromptResource;

  @Bean
  ChatClient chatClient(ChatClient.Builder chatClientBuilder) {
      return chatClientBuilder.defaultSystem(fixJsonResponsePromptResource).build();
  }

	public static void main(String[] args) {
		SpringApplication.run(RecipeFinderApplication.class, args);
	}

}
