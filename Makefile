# Fix-Me Makefile - Minimal

.PHONY: help
help:
	@echo "Fix-Me Build System"
	@echo ""
	@echo "Development:"
	@echo "  make dev          - Quick build + run (no tests)"
	@echo "  make build        - Build router only (no tests)"
	@echo "  make build-all    - Build all modules (no tests)"
	@echo "  make build-common - Build common + router"
	@echo "  make run          - Run router"
	@echo ""
	@echo "Production:"
	@echo "  make prod         - Full build with tests"
	@echo "  make test         - Run tests"
	@echo ""
	@echo "Utilities:"
	@echo "  make clean        - Clean build artifacts"
	@echo "  make restart      - Rebuild + run"

# Development
dev: build run

build:
	@mvn clean package -pl fix-router -DskipTests -q

build-all:
	@mvn clean package -DskipTests -q

build-common:
	@mvn clean install -pl fix-common -DskipTests -q
	@mvn clean package -pl fix-router -DskipTests -q

run:
	@java -jar fix-router/target/fix-router.jar

broker:
	@java -jar fix-broker/target/fix-broker-1.0.0-jar-with-dependencies.jar

market:
	@java -jar fix-market/target/fix-market-1.0-SNAPSHOT-jar-with-dependencies.jar
	
# Production
prod:
	@mvn clean install

test:
	@mvn test

# Utilities
clean:
	@mvn clean -q
	@rm -f router.pid router.log

restart: build run

.DEFAULT_GOAL := help