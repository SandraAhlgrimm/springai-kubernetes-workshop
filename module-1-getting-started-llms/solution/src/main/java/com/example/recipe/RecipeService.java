package com.example.recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
class RecipeService {

    private static final Logger log = LoggerFactory.getLogger(RecipeService.class);
    
    private final ChatClient chatClient;

    @Value("classpath:/prompts/recipe-for-ingredients")
    private Resource recipeForIngredientsPromptResource;

    RecipeService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    Recipe fetchRecipeFor(List<String> ingredients) {
        log.info("Fetch recipe for ingredients: {}", ingredients);

        return chatClient.prompt()
                .user(us -> us
                        .text(recipeForIngredientsPromptResource)
                        .param("ingredients", String.join(",", ingredients)))
                .call()
                // Enables structured output parsing
                .entity(Recipe.class);
    }

}
