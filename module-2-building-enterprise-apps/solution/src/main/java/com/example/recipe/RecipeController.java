package com.example.recipe;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RecipeController {

    private final RecipeService recipeService;

    RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/api/v1/recipes")
    public ResponseEntity<Recipe> fetchRecipeFor(@RequestParam List<String> ingredients) {
        var recipe = recipeService.fetchRecipeFor(ingredients);
        return ResponseEntity.ok(recipe);
    }
}
