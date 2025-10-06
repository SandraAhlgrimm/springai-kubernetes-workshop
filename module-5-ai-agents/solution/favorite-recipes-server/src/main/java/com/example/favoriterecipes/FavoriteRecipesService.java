package com.example.favoriterecipes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class FavoriteRecipesService {

	private static final Logger log = LoggerFactory.getLogger(FavoriteRecipesService.class);

	private final VectorStore vectorStore;

	FavoriteRecipesService(VectorStore vectorStore) {
		this.vectorStore = vectorStore;
	}

	@Tool(description = "Fetches the favorite recipes for a list of ingredients")
	public String fetchFavoriteRecipes(List<String> ingredients) {
		log.info("Fetching favorite recipes for ingredients from vector store: {}", ingredients);
		var searchRequest = SearchRequest.builder()
				.query(String.join(",", ingredients))
				.similarityThreshold(0.7).build();
		var documents = vectorStore.similaritySearch(searchRequest);
		log.info("{} documents found for ingredients: {}", documents.size(), ingredients);
		return documents.stream().map(Document::getText).collect(Collectors.joining(System.lineSeparator()));
	}

	public void addRecipeDocumentForRag(Resource pdfResource, int pageTopMargin, int pageBottomMargin) {
		log.info("Add recipe document {} for RAG", pdfResource.getFilename());
		var documentReaderConfig = PdfDocumentReaderConfig.builder()
				.withPageTopMargin(pageTopMargin)
				.withPageBottomMargin(pageBottomMargin)
				.build();
		var documentReader = new PagePdfDocumentReader(pdfResource, documentReaderConfig);
		var documents = new TokenTextSplitter().apply(documentReader.get());
		vectorStore.accept(documents);
	}
}
