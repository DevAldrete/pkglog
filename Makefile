# PkgLog - Sistema de Gestión de Logística y Envíos
MVN      ?= mvn
VERSION   = $(shell $(MVN) -q -DforceStdout help:evaluate -Dexpression=project.version 2>/dev/null)
JAR       = target/pkglog-$(VERSION).jar
MAIN     := com.dev.pkglog.App

.DEFAULT_GOAL := help
.PHONY: help compile test package run run-jar clean rebuild

help: ## Show available targets
	@grep -E '^[a-zA-Z_-]+:.*?## .*$$' $(MAKEFILE_LIST) | \
		awk 'BEGIN {FS = ":.*?## "}; {printf "  \033[36m%-12s\033[0m %s\n", $$1, $$2}'

compile: ## Compile the sources
	$(MVN) -B compile

test: ## Compile and run the unit tests
	$(MVN) -B test

package: ## Build the executable jar
	$(MVN) -B package

run: ## Run the app from source
	$(MVN) -B -q compile exec:java -Dexec.mainClass=$(MAIN)

run-jar: package ## Build and run the executable jar
	java -jar $(JAR)

clean: ## Remove build output
	$(MVN) -B clean

rebuild: clean package ## Clean and rebuild from scratch
