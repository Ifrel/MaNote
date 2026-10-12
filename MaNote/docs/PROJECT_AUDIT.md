# MaNote — Audit du projet existant

> Audit réalisé le 10/10/2026, **avant toute modification du code**.
> Ce document conserve le diagnostic initial et le plan d'implémentation retenu.
> L'état final après développement est décrit dans `README.md` et `docs/ARCHITECTURE.md`.

---

## 1. Analyse technique

### 1.1 Dépôt et arborescence

Le dépôt git a pour racine le dossier parent `MaNote/` (qui contient `LICENSE`, `.idea/` et le module Maven `MaNote/`).
Le projet Maven se trouve dans `MaNote/MaNote/`.

```
MaNote/                         ← racine git (LICENSE, .idea/)
└── MaNote/                     ← module Maven (pom.xml)
    ├── pom.xml
    ├── .gitignore
    ├── doc/uml/java/domain/entities/   (dossiers vides, "uml" est ignoré par .gitignore)
    ├── src/main/java/
    │   ├── MaNote.java                       point d'entrée (package par défaut)
    │   ├── api/
    │   │   ├── controllers/.gitkeep          vide
    │   │   ├── model/.gitkeep                vide
    │   │   └── views/
    │   │       ├── app/App.java              JFrame Swing
    │   │       ├── app/BarreMenu.java        JMenuBar factice ("Menu 1/2/3")
    │   │       ├── screens/ScreenPrincipal.java  JPanel vide
    │   │       └── widgets/NoteViewWidgets.java  JPanel affichant une note (jamais construit)
    │   ├── application/services/.gitkeep     vide
    │   ├── domain/entities/
    │   │   ├── Annee.java      @Entity  id:int, nom:String
    │   │   ├── Semestre.java   @Entity  id:int (généré), num:char
    │   │   ├── UE.java         @Entity  id:int (généré), nom, code, credit:int
    │   │   └── Note.java       @Entity  id:int (identity), nom, valeur:double, date:java.util.Date
    │   └── infrastructure/repositories/.gitkeep   vide
    └── target/                 artefacts de compilation (ignorés)
```

Aucun dossier `src/main/resources`, aucun dossier `src/test`.

### 1.2 Environnement et construction

| Élément                   | Constat                                                                                                                                                   |
|---------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------|
| Système de build          | **Maven** (`pom.xml`, groupId `org.manote`, artifactId `MaNote`, version `1.0-SNAPSHOT`)                                                                  |
| Version Java ciblée       | **Java 25** (`maven.compiler.source/target = 25`, IntelliJ `languageLevel=JDK_25`)                                                                        |
| JDK installé sur le poste | OpenJDK 26.0.2.1 (compatible avec la cible 25) — Maven 3.9.16                                                                                             |
| Plugins Maven             | aucun déclaré (plugins par défaut)                                                                                                                        |
| Dépendances               | `org.jetbrains:annotations:26.1.0`, `jakarta.persistence:jakarta.persistence-api:4.0.0-M7` (API seule, **aucun fournisseur JPA**, aucune base de données) |
| Framework graphique       | **Swing** (`javax.swing`)                                                                                                                                 |
| Point d'entrée            | `MaNote.main()` — méthode `static void main()` sans argument (syntaxe Java 25, JEP 512) qui ouvre `App` (JFrame 800×600)                                  |
| Tests                     | **aucun** (pas de JUnit, pas de dossier de tests)                                                                                                         |
| Ressources                | aucune (ni CSS, ni FXML, ni images, ni polices)                                                                                                           |
| Documentation             | aucune (pas de README) ; `doc/uml/...` contient uniquement des dossiers vides                                                                             |

Commandes existantes : `mvn compile` / `mvn package` (produit `target/MaNote-1.0-SNAPSHOT.jar`, sans manifeste
exécutable).

### 1.3 État git au moment de l'audit

Branche `main` à jour avec `origin/main` (5 commits « maquette »). Modifications locales **non commitées** :
`MaNote.java` (passage d'un `IO.println` à l'ouverture de la fenêtre Swing), `Annee.java` (annotations JPA),
`Note.java`, `UE.java`, `pom.xml` (indentation). Fichiers **non suivis** : `Semestre.java` et tout `api/views/**`.
Ces fichiers sont conservés tels quels (voir §4).

### 1.4 Architecture constatée

Le découpage des packages évoque une architecture en couches / hexagonale :

| Package                                       | Rôle supposé                | Contenu                        |
|-----------------------------------------------|-----------------------------|--------------------------------|
| `domain.entities`                             | Entités métier persistables | 4 entités JPA isolées          |
| `application.services`                        | Services applicatifs        | vide                           |
| `infrastructure.repositories`                 | Persistance                 | vide                           |
| `api.model` / `api.controllers` / `api.views` | Couche de présentation MVC  | seules les vues Swing existent |

Conventions : packages en anglais minuscules, **entités en français** (`Annee`, `Semestre`, `UE`, `Note`),
classes de vue mixtes (`App`, `BarreMenu`, `ScreenPrincipal`, `NoteViewWidgets`). Indentation 4 espaces.

### 1.5 Incohérences et défauts relevés

1. **Vue Swing non fonctionnelle** : `App` ajoute la `JMenuBar` comme *composant de contenu* (`add(barreMenu)`) au lieu
   de
   `setJMenuBar(...)` ; `ScreenPrincipal` est construit mais jamais ajouté ; `NoteViewWidgets.build()` n'est jamais
   appelé.
2. **Aucune relation entre entités** : rien ne relie `Annee` → `Semestre` → `UE` → `Note`.
3. **Pas de notion d'évaluation** : `Note` mélange l'évaluation (nom, date) et le résultat (valeur).
4. **`Note.valeur` est un `double` primitif** : impossible de représenter une note absente/en attente sans valeur
   magique (contraire à l'exigence « une note manquante n'est jamais zéro »).
5. **`Annee.id` n'a pas de getter** et pas de génération ; `Semestre.num` est un `char` (limite à 1 caractère).
6. Pas de statut d'évaluation, de barème, de coefficient, de type d'évaluation.
7. `jakarta.persistence-api` est une version *milestone* (`4.0.0-M7`) sans implémentation : les annotations sont
   décoratives.
8. Le `.gitignore` ignore `uml`, ce qui rend `doc/uml` non versionné.

---

## 2. Diagnostic

| # | Catégorie                               | Éléments                                                                                                                                                                                                                                                       |
|---|-----------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| 1 | **Ce qui fonctionne**                   | Compilation Maven ; ouverture d'une fenêtre Swing vide ; entités annotées JPA compilables.                                                                                                                                                                     |
| 2 | **Partiellement implémenté**            | Entités `Annee`, `Semestre`, `UE`, `Note` (attributs minimaux, sans relations) ; squelette de fenêtre Swing.                                                                                                                                                   |
| 3 | **Manquant**                            | Toute l'interface (navigation, écrans, formulaires) ; modèle d'évaluation et de statut ; contrat avec le moteur ; contrôleurs ; données ; design system ; tests ; documentation ; persistance des préférences.                                                 |
| 4 | **À corriger**                          | `JMenuBar` ajoutée comme contenu ; `Note.valeur` primitive (note absente impossible) ; absence de relations ; absence de getter `Annee.getId()`.                                                                                                               |
| 5 | **Conservable / réutilisable**          | Découpage des packages (`api` = présentation MVC, `application`, `domain`, `infrastructure`) ; entités du domaine (laissées au propriétaire du moteur) ; build Maven ; cible Java 25 ; point d'entrée `MaNote`.                                                |
| 6 | **Évolution architecturale nécessaire** | Introduire une **frontière explicite** (contrat Java) entre présentation et moteur ; DTO immuables indépendants de JPA et de l'UI ; composition root unique qui injecte le moteur ; framework graphique capable de répondre aux exigences visuelles (voir §3). |

---

## 3. Décision technologique : Swing → JavaFX

Le cahier des charges demande une interface « premium » (design system centralisé, thèmes clair/sombre, animations,
graphiques, états hover/focus soignés). Analyse :

| Critère                    | Swing (existant)                          | JavaFX                                             |
|----------------------------|-------------------------------------------|----------------------------------------------------|
| Code existant à migrer     | ~60 lignes de squelette sans comportement | —                                                  |
| Design system centralisé   | Look & Feel ou FlatLaf + code Java        | **CSS natif** avec couleurs « looked-up » (tokens) |
| Thèmes clair / sombre      | possible (FlatLaf)                        | échange d'une feuille de tokens à chaud            |
| Graphiques                 | aucun en standard (dépendance JFreeChart) | **`javafx.scene.chart` intégré**, stylable en CSS  |
| Animations                 | Timer manuel                              | `javafx.animation` intégré                         |
| Bindings réactifs          | non                                       | **propriétés et bindings**                         |
| Détection du thème système | non                                       | `Platform.getPreferences()` (JavaFX 22+)           |

**Décision : migrer la présentation vers JavaFX 25** (aligné sur la cible Java 25, compatible JDK 23+).
Le coût est négligeable (squelette Swing sans fonctionnalité) et le gain est majeur. Les classes Swing existantes
(`App`, `BarreMenu`, `ScreenPrincipal`, `NoteViewWidgets`) **ne sont pas supprimées** : elles restent compilables
(module `java.desktop` du JDK) mais ne sont plus utilisées par le point d'entrée. Leur suppression est laissée à
l'appréciation de l'équipe.

Vues construites **en Java** (sans FXML) : la plupart des écrans sont composés dynamiquement à partir des données
(cartes d'UE, listes d'échéances, formulaires de configuration générés par le moteur). Des composants Java typés offrent
une meilleure réutilisation et une vérification à la compilation ; la séparation structure/style est assurée par le CSS.

### Dépendances introduites (justifiées)

| Dépendance                                                        | Raison                                                                                                      |
|-------------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------|
| `org.openjfx:javafx-controls` 25.0.4                              | Framework graphique                                                                                         |
| `org.kordamp.ikonli:ikonli-javafx` + `ikonli-feather-pack` 12.4.0 | Jeu d'icônes homogène et léger (polices vectorielles), vérifié à la compilation via l'énumération `Feather` |
| `org.junit.jupiter:junit-jupiter` (test)                          | Tests unitaires                                                                                             |
| Police **Inter** (OFL) embarquée en ressource                     | Typographie moderne et identique sur toutes les plateformes                                                 |

Pas de framework d'injection, pas de base de données, pas de serveur HTTP.

---

## 4. Plan d'implémentation

### 4.1 Packages

| Package                      | Contenu                                                                                                       |
|------------------------------|---------------------------------------------------------------------------------------------------------------|
| `domain.entities`            | **Inchangé** (propriété du moteur métier)                                                                     |
| `application.engine`         | Contrat `GradeEngine`, `EngineInfo`, `EngineCapabilities`                                                     |
| `application.engine.model`   | DTO immuables (records) : identifiants typés, résumés, détails, brouillons, résultats calculés, configuration |
| `application.engine.error`   | Exceptions métier du contrat                                                                                  |
| `infrastructure.demo`        | `DemoGradeEngine` (données fictives réalistes, en mémoire)                                                    |
| `infrastructure.engine`      | `EngineLoader` (sélection de l'implémentation : propriété système → `ServiceLoader` → démo)                   |
| `infrastructure.preferences` | Persistance des préférences (`java.util.prefs`)                                                               |
| `api.model`                  | Modèles de présentation : paramètres, contexte académique, états de chargement, destinations                  |
| `api.controllers`            | Contrôleurs d'écran, navigation, exécution asynchrone, messages d'erreur                                      |
| `api.views.app`              | Application JavaFX, `AppShell`, `SidebarNavigation`, `TopBar`, hôte de modales                                |
| `api.views.screens`          | Les 10 écrans                                                                                                 |
| `api.views.dialogs`          | Formulaires et confirmations                                                                                  |
| `api.views.widgets`          | Bibliothèque de composants réutilisables                                                                      |
| `api.views.charts`           | Graphiques                                                                                                    |
| `api.views.format`           | Formatage des notes, dates et libellés                                                                        |

### 4.2 Écrans

Tableau de bord · Semestres & années · Détail d'UE · Évaluations (tableau + chronologie) · Formulaire d'évaluation ·
Saisie de note · Analyse & progression (dont simulateur) · Planification (calendrier + échéancier) ·
Paramétrage du moteur · Paramètres généraux.

### 4.3 Contrat moteur (principes)

* Interface unique `GradeEngine`, **synchrone**, appelée exclusivement depuis un thread de travail dédié et sérialisé
  (le moteur n'a pas besoin d'être thread-safe ni de connaître JavaFX).
* Les opérations facultatives (structure, simulation, configuration, chronologie de moyenne) ont une implémentation
  par défaut qui lève `OperationNotSupportedException` ; l'UI consulte `EngineCapabilities` pour les proposer ou
  afficher
  un état « non disponible ».
* Les résultats calculés sont des types scellés (`ComputedResult` : valeur / non calculable / non fournie / erreur),
  avec
  une **nature** explicite (définitive, provisoire, simulée). Les notes sont un type scellé (`GradeEntry` : score /
  absence) ;
  l'absence de note est `Optional.empty()` + un statut explicite — jamais `0`.
* La configuration du calcul est **auto-décrite par le moteur** (sections et paramètres typés) : l'UI génère le
  formulaire
  sans connaître les règles universitaires ni imposer un langage de formules.

### 4.4 Tests et validation

* Tests JUnit du contrat via le moteur de démonstration (cohérence des données, statuts, notes manquantes, erreurs).
* Tests des contrôleurs avec un exécuteur synchrone (aucun thread JavaFX requis).
* Tests du formatage, de la validation des formulaires et de la sélection du moteur.
* Validation visuelle : outil de capture qui rend chaque écran hors écran (snapshot JavaFX) en thème clair et sombre,
  utilisé pour contrôler le rendu et produire les captures du README.

### 4.5 Étapes

A. Audit (ce document) → B. Fondations (contrat, DTO, démo, injection, compilation, tests) → C. Design system et shell →
D. Écrans principaux (compilation et vérification après chaque écran) → E. Analyses et graphiques →
F. Documentation, tests, captures, vérification du lancement.

---

## 5. Bilan Post-Développement & Validation Finale

Toutes les étapes du plan et de l'intégration des données réelles ont été réalisées et validées :

1. **Architecture MVC Desktop & Inversion de dépendance** :
    - Présentation complète sous JavaFX 25 (`api.views.*`), animée par `AppController` et son worker thread
      d'arrière-plan (`MaNote-Engine-Worker`).
    - Arrêt propre du cycle de vie lors de la fermeture de la fenêtre (`MaNoteApp.stop()` invoque
      `AppController.shutdown()`).
    - Contrat métier synchrone et étanche (`application.engine.GradeEngine`), découplé de JavaFX.
    - Entités du domaine (`Annee`, `Semestre`, `UE`, `Note`) complètement enrichies avec annotations JPA, relations
      `@OneToMany` / `@ManyToOne`, champs d'évaluations et constructeurs.

2. **Persistance Relationnelle Complète & Données Réelles** :
    - Intégration de Hibernate ORM 6.6.2.Final et de la base embarquée H2 Database 2.3.232.
    - Base de données locale fichier persistée dans `~/.manote/manote.mv.db` avec `AUTO_SERVER=TRUE`.
    - Gestionnaire de sessions transactionnel `infrastructure.persistence.JpaManager`.
    - Seeding automatique du cursus de Master 1 Informatique (Semestres 7 & 8, 8 UEs, devoirs, examens et notes réelles)
      via `infrastructure.persistence.DatabaseInitializer`.
    - Dépôts relationnels JPA dédiés : `AnneeRepository`, `SemestreRepository`, `UeRepository`, `NoteRepository`.
    - Moteur persistant par défaut `application.engine.JpaGradeEngine`, épaulé par
      `application.services.GradeCalculationService` pour les moyennes pondérées, les chronologies et les simulations.
    - Le moteur de démonstration en mémoire `DemoGradeEngine` est conservé pour les tests et le mode bac à sable
      (`--engine=demo`).

3. **Système de Build & Makefile** :
    - Deux `Makefiles` (`/Makefile` à la racine et `/MaNote/Makefile` dans le module) permettent d'exécuter `make run`,
      `make test`, `make package`, `make compile`, `make check`, `make lint`, `make clean` et `make all`.

4. **Validation & Tests Automatisés** :
    - **30 tests unitaires JUnit 5** exécutés avec 0 échec et 0 erreur :
        - Moteur persistant JPA relationnel et cycle de vie CRUD (`JpaGradeEngineTest` : 8 tests),
        - Contrat du moteur de calcul (`DemoGradeEngineTest` : 7 tests),
        - Formateurs et garantie qu'une note absente n'est jamais zéro (`FormattersTest` : 7 tests),
        - Entités JPA du domaine et intégrité relationnelle (`DomainEntitiesTest` : 5 tests),
        - Injection dynamique et repli du moteur (`EngineLoaderTest` : 3 tests).
    - Compilation rigoureuse sous Java 25 et Maven avec 0 erreur.

