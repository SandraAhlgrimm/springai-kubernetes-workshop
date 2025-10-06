#!/bin/bash
set -e

echo "🏗️  Building all microservices..."

echo "📦 Building fridge-server..."
(cd fridge-server && ./gradlew clean build -x test)

echo "📦 Building favorite-recipes-server..."
(cd favorite-recipes-server && ./gradlew clean build -x test)

echo "📦 Building recipe-finder-client..."
(cd recipe-finder-client && ./gradlew clean build -x test)

echo ""
echo "🚀 Starting all services with Docker Compose..."
docker compose up --build -d

echo ""
echo "⏳ Waiting for Ollama to be ready..."
sleep 5

echo "📥 Pulling qwen3:1.7b model..."
docker exec ollama ollama pull qwen3:1.7b

echo "📥 Pulling nomic-embed-text model..."
docker exec ollama ollama pull nomic-embed-text

echo ""
echo "✅ All services started successfully!"
echo ""
echo "📍 Service URLs:"
echo "   - Recipe Finder UI:       http://localhost:8080"
echo "   - Fridge Server:          http://localhost:8081"
echo "   - Favorite Recipes Server: http://localhost:8082"
echo "   - Redis Insight:          http://localhost:8001"
echo "   - Ollama API:             http://localhost:11434"
echo ""
echo "📝 To upload recipe PDFs for RAG:"
echo "   curl -X POST -F 'file=@sample-recipes.pdf' -F 'pageBottomMargin=50' http://localhost:8082/api/v1/recipes/upload"
echo ""
echo "🛑 To stop all services:"
echo "   docker compose down"
