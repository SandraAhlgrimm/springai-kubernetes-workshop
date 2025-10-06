package com.example.fridge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FridgeService {

	private static final Logger log = LoggerFactory.getLogger(FridgeService.class);

	@Value("${app.available-ingredients-in-fridge}")
	private List<String> availableIngredientsInFridge;

	@Tool(description = "Fetches ingredients that are available in the fridge")
	public List<String> fetchIngredientsAvailableInFridge() {
		log.info("Fetching ingredients that are available in the fridge");
		return availableIngredientsInFridge;
	}
}
