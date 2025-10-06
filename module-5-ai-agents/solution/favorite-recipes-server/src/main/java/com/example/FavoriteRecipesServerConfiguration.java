package com.example;

import com.example.favoriterecipes.FavoriteRecipesService;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class FavoriteRecipesServerConfiguration {

	@Bean
	ToolCallbackProvider favoriteRecipesTools(FavoriteRecipesService favoriteRecipesService) {
		return MethodToolCallbackProvider.builder().toolObjects(favoriteRecipesService).build();
	}

	@ConditionalOnMissingBean(VectorStore.class)
	@Bean
	VectorStore simpleVectorStore(EmbeddingModel embeddingModel) {
		return SimpleVectorStore.builder(embeddingModel).build();
	}
}
