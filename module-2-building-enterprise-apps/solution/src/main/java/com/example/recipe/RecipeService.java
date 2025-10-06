package com.example.recipe;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class RecipeService {

    private final ChatClient chatClient;

    @Value("classpath:/prompts/recipe-for-ingredients")
    private Resource recipeForIngredientsPromptResource;

    RecipeService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    Recipe fetchRecipeFor(List<String> ingredients) {
        return chatClient.prompt()
                .user(us -> us
                        .text(recipeForIngredientsPromptResource)
                        .param("ingredients", String.join(",", ingredients)))
                .call()
                 // Enables structured output parsing
                .entity(Recipe.class);
    }
}