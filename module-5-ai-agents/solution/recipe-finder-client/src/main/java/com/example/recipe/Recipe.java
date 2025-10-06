package com.example.recipe;

import java.util.List;

record Recipe(String name, String description, List<String> ingredients, List<String> instructions) {
}
