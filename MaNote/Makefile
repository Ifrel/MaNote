# ==============================================================================
# MaNote — Gestionnaire de Notes Universitaires (JavaFX 25 / Java 25)
# ==============================================================================
# Usage :
#   make              Affiche l'aide
#   make run          Lance l'application graphique JavaFX
#   make test         Exécute la suite de tests JUnit 5
#   make package      Construit le JAR exécutable dans target/
#   make clean        Supprime les fichiers générés
# ==============================================================================

.PHONY: help all compile build test check package run run-swing clean verify lint

MVN ?= mvn

# Couleur et mise en forme du terminal
CYAN  := \033[36m
GREEN := \033[32m
YELLOW:= \033[33m
RESET := \033[0m
BOLD  := \033[1m

default: help

## help: Affiche la liste des cibles disponibles
help:
	@echo "$(BOLD)$(CYAN)MaNote — Système de Build Make$(RESET)"
	@echo "Commandes disponibles :"
	@echo "  $(GREEN)make run$(RESET)         Lance l'application de bureau JavaFX moderne"
	@echo "  $(GREEN)make run-swing$(RESET)   Lance l'ancienne vue Swing (compatibilité)"
	@echo "  $(GREEN)make test$(RESET)        Exécute la suite de tests unitaires JUnit 5"
	@echo "  $(GREEN)make compile$(RESET)     Compile les sources de l'application"
	@echo "  $(GREEN)make package$(RESET)     Compile, teste et génère le fichier JAR"
	@echo "  $(GREEN)make check$(RESET)       Vérifie l'intégrité du projet (tests unitaires)"
	@echo "  $(GREEN)make lint$(RESET)        Compile avec les avertissements stricts du compilateur"
	@echo "  $(GREEN)make clean$(RESET)       Supprime le répertoire de build target/"
	@echo "  $(GREEN)make all$(RESET)         Exécute clean, compile, test et package"

## compile: Compile les classes Java du projet
compile:
	@echo "$(CYAN)--> Compilation des sources Java...$(RESET)"
	$(MVN) compile

## build: Alias pour package
build: package

## test: Exécute la suite de tests unitaires JUnit 5
test:
	@echo "$(CYAN)--> Exécution des tests unitaires...$(RESET)"
	$(MVN) test

## check: Vérifie la validité des tests
check: test

## package: Compile et produit le package JAR
package:
	@echo "$(CYAN)--> Génération du package JAR...$(RESET)"
	$(MVN) clean package

## run: Lance l'application de bureau JavaFX
run:
	@echo "$(GREEN)--> Lancement de MaNote (JavaFX)...$(RESET)"
	$(MVN) javafx:run

## run-swing: Lance le mode de compatibilité Swing
run-swing:
	@echo "$(YELLOW)--> Lancement de MaNote (Mode Swing)...$(RESET)"
	$(MVN) javafx:run -Djavafx.args="--swing"

## lint: Compile avec l'ensemble des diagnostics javac
lint:
	@echo "$(CYAN)--> Analyse statique du code (javac -Xlint)...$(RESET)"
	$(MVN) test-compile -DcompilerArgument="-Xlint:all"

## verify: Exécute la phase de vérification Maven complète
verify:
	@echo "$(CYAN)--> Vérification complète Maven...$(RESET)"
	$(MVN) verify

## clean: Supprime les artefacts de compilation
clean:
	@echo "$(YELLOW)--> Nettoyage des artefacts (target/)...$(RESET)"
	$(MVN) clean

## all: Exécute clean, compile, test et package
all: clean compile test package
	@echo "$(GREEN)--> Build complet terminé avec succès !$(RESET)"
