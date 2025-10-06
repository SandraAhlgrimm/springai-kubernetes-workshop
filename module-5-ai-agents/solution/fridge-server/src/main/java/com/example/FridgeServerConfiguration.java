package com.example;

import com.example.fridge.FridgeService;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FridgeServerConfiguration {

	@Bean
	ToolCallbackProvider fridgeTools(FridgeService fridgeService) {
		return MethodToolCallbackProvider.builder().toolObjects(fridgeService).build();
	}
}
