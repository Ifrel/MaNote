# MaNote — Gestionnaire Académique de Notes Universitaires (Java Desktop)

[![Java 25](https://img.shields.io/badge/Java-25-orange.svg)](https://openjdk.org/)
[![JavaFX 25](https://img.shields.io/badge/JavaFX-25.0.4-blue.svg)](https://openjfx.io/)
[![Hibernate ORM](https://img.shields.io/badge/Hibernate-6.6.2-59666C.svg)](https://hibernate.org/)
[![H2 Database](https://img.shields.io/badge/H2-2.3.232-blue.svg)](https://www.h2database.com/)
[![Maven 3.9](https://img.shields.io/badge/Build-Maven-C71A36.svg)](https://maven.apache.org/)
[![Makefile](https://img.shields.io/badge/Make-Automated-green.svg)]()
[![Tests](https://img.shields.io/badge/Tests-30%20pass-success.svg)]()

**MaNote** est une application de bureau complète, moderne et élégante conçue en **Java / JavaFX** pour permettre à un
étudiant (notamment en Master Informatique) de piloter ses études : suivi des notes, gestion des unités d'enseignement
(UE), planification des examens et échéances, analyse graphique de progression et simulation de moyennes futures.

L'application est connectée nativement à un **moteur de persistance relationnelle complet (JPA / Hibernate / base H2
locale)** et applique une **séparation stricte des responsabilités (Architecture MVC)** : le moteur métier et de calcul
est strictement découplé de la présentation via le contrat `GradeEngine`.

---

## 📸 Fonctionnalités Principales

1. **Persistance Relationnelle & Cursus Personnalisable** :
    - Stockage local dans la base de données relationnelle embarquée H2 (`~/.manote/manote.mv.db`).
    - **Installation vierge et sans fausses données** : la base est prête pour vos saisies réelles.
    - **Configuration flexible** :
        - Soit directement dans l'interface graphique : ajout et gestion visuelle de vos Années, Semestres, UE et DS
          avec leurs pourcentages et barèmes.
        - Soit via un fichier texte YAML éditable : `~/.manote/cursus.yaml` (modèle fourni dans
          `~/.manote/cursus.yaml.template`). L'application synchronise automatiquement le fichier au démarrage ou via le
          bouton « Synchroniser YAML ».
    - Toutes les opérations (création d'évaluations, saisie de notes, modifications de pondérations ou de seuils) sont
      persistées de manière transactionnelle.
2. **Tableau de Bord Intuitif** : Synthèse immédiate du semestre sélectionné, cartes statistiques clés (moyenne pondérée
   calculée par le moteur, ECTS validés, progression des évaluations, alertes), grille interactive des UE, dernières
   notes obtenues et prochaines échéances.
3. **Parcours & Historique des Semestres** : Navigation entre les années académiques (ex. Licence 3 vs Master 1),
   isolation stricte des données historiques et consultation des validations.
4. **Vue Détaillée d'une UE** : Tableau complet des évaluations (Quiz, TP, CC, Projets, Examens), notes obtenues,
   barèmes, notes ramenées sur 20, coefficients, contributions au résultat final et explications de calcul fournies par
   le moteur.
5. **Gestion Complète des Évaluations** : Recherche textuelle instantanée, filtrage multi-critères (par UE, par type,
   par statut), tri dynamique et actions rapides d'édition et suppression.
6. **Saisie des Notes & Traitement des Absences** :
    - Saisie d'une note chiffrée sur barème avec validation stricte des bornes.
    - Déclaration d'une absence justifiée (ABJ) ou injustifiée (ABI).
    - **Règle fondamentale** : une évaluation en attente de note ou non notée n'est **jamais assimilée à zéro**.
7. **Analyse Graphique & Simulateur de Moyennes** :
    - Diagramme en barres comparatif des moyennes par UE.
    - Graphique linéaire de l'évolution chronologique de la moyenne au cours du semestre.
    - **Simulateur d'estimations** : testez des hypothèses de notes sur les devoirs à venir avec affichage explicite de
      la mention `SIMULATED`.
8. **Planification des Échéances** : Vue temporelle organisée par urgence (7 prochains jours, dans le mois, plus tard,
   dates indéterminées).
9. **Paramétrage Dynamique du Moteur** : Écran de configuration auto-généré à partir des spécifications du moteur
   (seuils de validation, règles d'absences, compensation).
10. **Système de Design Desktop Premium** :
- Thème Clair et Thème Sombre commutables en un clic avec persistance.
- Typographie soignée basée sur la police embarquée **Inter (OFL)**.
- Jeu d'icônes vectorielles **Feather** via Ikonli.
- Notifications toast animées et fenêtres redimensionnables adaptées au bureau.

---

## 🛠️ Technologies Utilisées

| Composant                | Technologie                                                                         |
|--------------------------|-------------------------------------------------------------------------------------|
| Langage                  | **Java 25** (OpenJFX 25.0.4)                                                        |
| Système de Build         | **GNU Make** & **Apache Maven**                                                     |
| Framework Graphique      | **JavaFX Controls** & **JavaFX Charts**                                             |
| Persistance & ORM        | **Hibernate ORM 6.6.2.Final** (Jakarta Persistence 3.1)                             |
| Base de Données          | **H2 Database 2.3.232** (fichier local avec auto-server & in-memory pour les tests) |
| Icônes Vectorielles      | **Ikonli Feather Pack** (12.4.0)                                                    |
| Typographie              | **Inter** (Regular, Medium, SemiBold, Bold) embarquée                               |
| Persistance des Réglages | `java.util.prefs.Preferences` (standard Java, multiplateforme)                      |
| Tests Unitaires          | **JUnit 5 Jupiter** (30 tests unitaires automatisés)                                |

---

## 🚀 Commandes Rapides (Makefile)

Un `Makefile` complet est disponible à la racine du dépôt ainsi que dans le dossier du projet :

| Commande              | Action                                                                         |
|-----------------------|--------------------------------------------------------------------------------|
| `make` ou `make help` | Affiche l'aide avec les cibles disponibles                                     |
| `make run`            | **Lance l'application de bureau JavaFX connectée à la base H2**                |
| `make test`           | **Exécute la suite complète de 30 tests unitaires JUnit 5**                    |
| `make package`        | Compile, teste et génère le fichier JAR exécutable                             |
| `make compile`        | Compile les sources de l'application                                           |
| `make check`          | Vérifie l'intégrité du projet (tests unitaires)                                |
| `make lint`           | Compile avec l'ensemble des avertissements du compilateur (`javac -Xlint:all`) |
| `make clean`          | Nettoie les fichiers compilés (`target/`)                                      |
| `make all`            | Enchaîne `clean`, `compile`, `test` et `package`                               |
| `make run-swing`      | Lance l'ancienne vue Swing (compatibilité conservée)                           |

*(Vous pouvez également utiliser les commandes Maven équivalentes, ex. `mvn test`, `mvn javafx:run`)*.

---

## 🔌 Raccordement et Moteurs de Calcul

L'application démarre par défaut sur **`JpaGradeEngine`**, connecté à la base relationnelle locale H2 et appliquant les
calculs normalisés universitaires (moyennes pondérées, validation, timeline, simulations).

### Moteurs disponibles & alternatives :

1. **Moteur Réel Persistant par défaut (`JpaGradeEngine`)** :
    - Sauvegarde les entités `Annee`, `Semestre`, `UE`, `Note` dans `~/.manote/manote.mv.db`.
    - Fournit toutes les capacités (édition structurelle, simulations, chronologie de moyenne, paramétrage dynamique).
2. **Moteur de Démonstration (`DemoGradeEngine`)** :
    - Mode bac à sable en mémoire pure avec données fixes.
    - Activé simplement via `--engine=demo` ou `-Dmanote.engine.class=demo`.
3. **Votre propre moteur personnalisé** :
    - Implémentez l'interface `application.engine.GradeEngine` (guide dans [
      `docs/ENGINE_CONTRACT.md`](docs/ENGINE_CONTRACT.md)).
    - Spécifiez votre classe via :
      ```bash
      mvn javafx:run -Djavafx.args="--engine=mon.package.MonGradeEngine"
      ```
      ou via `META-INF/services/application.engine.GradeEngine`.

---

## 📂 Architecture des Packages

```
src/main/java/
├── MaNote.java                              ← Point d'entrée exécutable principal (CLI, JavaFX, Swing)
├── api/
│   ├── controllers/
│   │   └── AppController.java               ← Contrôleur MVC, dispatch asynchrone sur worker thread
│   ├── model/
│   │   ├── AppContext.java                  ← État réactif (année/semestre actif, thème, toasts)
│   │   └── NavigationDestination.java       ← Écrans disponibles
│   └── views/
│       ├── app/
│       │   ├── MaNoteApp.java               ← Application JavaFX & cycle de vie
│       │   ├── AppShell.java                ← Coquille principale (Sidebar + TopBar + Contenu)
│       │   ├── App.java                     ← Fenêtre Swing (mode compatibilité)
│       │   └── BarreMenu.java               ← Menu Swing (mode compatibilité)
│       ├── screens/                         ← Les écrans fonctionnels complets (Dashboard, UeDetails, ...)
│       ├── widgets/                         ← Composants UI réutilisables (StatCard, UECard, Badge...)
│       ├── dialogs/                         ← Dialogues d'évaluation, de note et de confirmation
│       └── format/Formatters.java           ← Formatage des notes et dates en français
├── application/
│   ├── engine/
│   │   ├── GradeEngine.java                 ← Contrat d'interface du moteur de calcul
│   │   ├── JpaGradeEngine.java              ← Implémentation réelle persistante connectée à JPA
│   │   ├── EngineInfo.java / Capabilities   ← Capacités et métadonnées du moteur
│   │   ├── model/                           ← DTOs immuables (records) indépendants de l'UI
│   │   ├── config/                          ← Configuration dynamique auto-décrite
│   │   └── error/                           ← Hiérarchie d'exceptions métier explicites
│   └── services/
│       └── GradeCalculationService.java     ← Service métier de calcul des moyennes et simulations
├── infrastructure/
│   ├── persistence/
│   │   ├── JpaManager.java                  ← SessionFactory Hibernate, configuration H2
│   │   └── DatabaseInitializer.java         ← Initialiseur du cursus M1 si base vierge
│   ├── repositories/
│   │   ├── AnneeRepository.java             ← Dépôt JPA Années
│   │   ├── SemestreRepository.java          ← Dépôt JPA Semestres
│   │   ├── UeRepository.java                ← Dépôt JPA UEs
│   │   └── NoteRepository.java              ← Dépôt JPA Notes / Évaluations
│   ├── engine/EngineLoader.java             ← Injection dynamique du moteur (CLI / Property / ServiceLoader)
│   ├── demo/DemoGradeEngine.java            ← Moteur de démonstration alternatif en mémoire
│   ├── demo/DemoData.java                   ← Dataset déterministe de démo
│   └── preferences/UserPreferences.java     ← Sauvegarde locale du thème et des réglages
└── domain/entities/                         ← Entités JPA du domaine (Annee, Semestre, UE, Note)
```

---

## 📖 Documentation Complète

- **Diagnostic et audit exhaustif du code** : [`docs/PROJECT_AUDIT.md`](docs/PROJECT_AUDIT.md)
- **Spécification des exigences logicielles (SRS)** : [`docs/SRS.md`](docs/SRS.md)
- **Architecture logicielle & flux MVC** : [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md)
- **Design System & Guide de style** : [`docs/UI_DESIGN.md`](docs/UI_DESIGN.md)
- **Contrat technique d'intégration du moteur** : [`docs/ENGINE_CONTRACT.md`](docs/ENGINE_CONTRACT.md)
