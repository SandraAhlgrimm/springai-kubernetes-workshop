package com.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

@Configuration
class RecipeFinderClientConfiguration {

	@Value("classpath:/prompts/system-fix-json-response.txt")
	private Resource fixJsonResponsePromptResource;

	@Bean
	ChatClient chatClient(ChatClient.Builder chatClientBuilder, ToolCallbackProvider toolsCallbacks) {
		return chatClientBuilder
				.defaultToolCallbacks(toolsCallbacks)
				.defaultSystem(fixJsonResponsePromptResource)
				.build();
	}
}
