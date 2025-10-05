package com.example.recipe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
class RecipeResource {

    private final RecipeService recipeService;

    RecipeResource(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping
    Recipe fetchRecipeFor(@RequestParam String ingredients) {
        List<String> ingredientsList = Arrays.asList(ingredients.split("\\s*,\\s*"));
        return recipeService.fetchRecipeFor(ingredientsList);
    }

}
