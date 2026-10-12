# ==============================================================================
# MaNote — Makefile racine du dépôt
# ==============================================================================
# Redirige toutes les commandes vers le projet Maven situé dans ./MaNote
# ==============================================================================

.PHONY: help all compile build test check package run run-swing clean verify lint

SUBDIR := MaNote

default: help

help:
	@$(MAKE) -C $(SUBDIR) help

compile:
	@$(MAKE) -C $(SUBDIR) compile

build:
	@$(MAKE) -C $(SUBDIR) build

test:
	@$(MAKE) -C $(SUBDIR) test

check:
	@$(MAKE) -C $(SUBDIR) check

package:
	@$(MAKE) -C $(SUBDIR) package

run:
	@$(MAKE) -C $(SUBDIR) run

run-swing:
	@$(MAKE) -C $(SUBDIR) run-swing

lint:
	@$(MAKE) -C $(SUBDIR) lint

verify:
	@$(MAKE) -C $(SUBDIR) verify

clean:
	@$(MAKE) -C $(SUBDIR) clean

all:
	@$(MAKE) -C $(SUBDIR) all
