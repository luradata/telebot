# Define required directories
REQUIRED_DIRS := ollama-models

# Create directories
init:
	@echo "Creating required directories..."
	@mkdir -p $(REQUIRED_DIRS)
	@chmod 777 ollama-models
	@echo "Directories created successfully!"

# Clean up
clean:
	@echo "Cleaning up..."
	@if docker image ls | grep -q telebot-app; then \
		docker image rm telebot-app; \
		echo "Removed telebot-app image"; \
	fi
	@echo "Cleaned up successfully!"

# Docker compose commands
up:
	@echo "Starting containers..."
	docker compose up -d

down:
	@echo "Stopping and removing containers..."
	docker compose down

restart:
	@echo "Restarting containers..."
	docker compose restart

logs:
	@echo "Viewing logs of containers..."
	docker compose logs -f

ps:
	@echo "Listing running containers..."
	docker compose ps

# Display help
help:
	@echo "Available commands:"
	@echo "  make init     : Create required directories"
	@echo "  make clean   : Clean up logs and tmp directories"
	@echo "  make up      : Start docker containers"
	@echo "  make down    : Stop docker containers"
	@echo "  make restart : Restart docker containers"
	@echo "  make logs    : View logs of containers"
	@echo "  make ps      : List running containers"
	@echo "  make help    : Display this help"

.PHONY: init clean up down restart logs ps help