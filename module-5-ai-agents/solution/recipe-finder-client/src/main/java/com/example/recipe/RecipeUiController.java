package com.example.recipe;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
class RecipeUiController {

    private static final Logger log = LoggerFactory.getLogger(RecipeUiController.class);

    private final RecipeService recipeService;
    private final ChatModel chatModel;

    RecipeUiController(RecipeService recipeService, ChatModel chatModel) {
        this.recipeService = recipeService;
        this.chatModel = chatModel;
    }

    @GetMapping
    String fetchUI(Model model) {
        model.addAttribute("aiModel", "Qwen 3 (1.7B)");
        if (!model.containsAttribute("fetchRecipeData")) {
            model.addAttribute("fetchRecipeData", new FetchRecipeData());
        }
        return "index";
    }

    @PostMapping
    String fetchRecipeUiFor(FetchRecipeData fetchRecipeData, Model model) {
        Recipe recipe;
        try {
            recipe = recipeService.fetchRecipeFor(
                    fetchRecipeData.ingredients(), 
                    fetchRecipeData.isPreferAvailableIngredients(), 
                    fetchRecipeData.isPreferOwnRecipes());
        } catch (Exception e) {
            log.warn("Retry RecipeUiController:fetchRecipeFor after exception caused by LLM", e);
            recipe = recipeService.fetchRecipeFor(
                    fetchRecipeData.ingredients(), 
                    fetchRecipeData.isPreferAvailableIngredients(), 
                    fetchRecipeData.isPreferOwnRecipes());
        }
        model.addAttribute("recipe", recipe);
        model.addAttribute("fetchRecipeData", fetchRecipeData);
        return fetchUI(model);
    }
}
