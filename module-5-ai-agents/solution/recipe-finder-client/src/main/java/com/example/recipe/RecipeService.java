package com.example.recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
class RecipeService {

	private static final Logger log = LoggerFactory.getLogger(RecipeService.class);

	private final ChatClient chatClient;

	@Value("classpath:/prompts/recipe-for-ingredients.txt")
	private Resource recipeForIngredientsPromptResource;

	@Value("classpath:/prompts/system-use-ingredients-from-fridge.txt")
	private Resource useIngredientsFromFridgePromptResource;

	@Value("classpath:/prompts/system-dont-use-specific-tool.txt")
	private Resource dontUseSpecificToolPromptResource;

	@Value("classpath:/prompts/system-prefer-own-recipe.txt")
	private Resource preferOwnRecipePromptResource;

	RecipeService(ChatClient chatClient) {
		this.chatClient = chatClient;
	}

	Recipe fetchRecipeFor(List<String> ingredients, boolean preferAvailableIngredients, boolean preferOwnRecipes) {
		log.info("Fetching recipe for ingredients: {}, preferAvailableIngredients: {}, preferOwnRecipes: {}", 
				ingredients, preferAvailableIngredients, preferOwnRecipes);
		
		return chatClient.prompt()
				.user(us -> us
					.text(recipeForIngredientsPromptResource)
					.param("ingredients", String.join(",", ingredients)))
				.system(getSystemPrompt(preferAvailableIngredients, preferOwnRecipes))
				.call()
				.entity(Recipe.class);
	}

	private String getSystemPrompt(boolean preferAvailableIngredients, boolean preferOwnRecipes) {
		var promptText = "";
		if (preferAvailableIngredients) {
			promptText += new PromptTemplate(useIngredientsFromFridgePromptResource).render();
		} else {
			promptText += new PromptTemplate(dontUseSpecificToolPromptResource)
					.render(Map.of("tool", "fetchIngredientsAvailableInFridge"));
		}

		if (preferOwnRecipes) {
			promptText += new PromptTemplate(preferOwnRecipePromptResource).render();
		} else {
			promptText += new PromptTemplate(dontUseSpecificToolPromptResource)
					.render(Map.of("tool", "fetchFavoriteRecipes"));
		}
		return promptText;
	}
}
