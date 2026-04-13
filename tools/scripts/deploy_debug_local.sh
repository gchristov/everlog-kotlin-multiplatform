# Builds and deploys the project to Firebase locally with Docker
set -e
echo "🛠 Build project" && ./gradlew assembleDebug
echo "🧹 Clean up old Docker resources" && (docker image prune -af)
echo "🏗 Build Docker image" && docker build -t everlog-debug -f mobile/src/debug/Dockerfile .
echo "🚀 Run Docker image" && docker run --rm -v $(pwd):/app everlog-debug
