# Contrat d'Intégration du Moteur Métier de Calcul (`GradeEngine`)

> Ce document s'adresse au responsable de l'implémentation du moteur de calcul des notes universitaires.
> Il détaille l'interface Java, les objets échangés, le traitement des notes manquantes et les instructions pour
> brancher le moteur réel à l'interface graphique.

---

## 1. Principes Fondamentaux de la Frontière Métier

1. **Source unique de vérité** : L'interface graphique (`api.*`) **ne calcule aucune moyenne**, ne connaît pas les
   règles universitaires et n'interprète pas les seuils de validation. Tous les résultats numériques, statuts et
   validations proviennent du moteur via le contrat `GradeEngine`.
2. **Une note manquante n'est jamais assimilée à zéro** :
    - Tant qu'une évaluation n'a pas été notée, son statut est `PLANNED` (planifiée) ou `AWAITING_GRADE` (en attente).
    - L'évaluation porte un `Optional.empty()` pour son résultat `GradeEntry`.
    - Sa note normalisée est un `ComputedResult.NotComputable("Note non disponible")`.
    - Le moteur décide s'il peut calculer une moyenne provisoire ou s'il doit retourner
      `ComputedResult.notComputable("Notes insuffisantes")`.
3. **Exécution synchrone & Sérialisée** :
    - Toutes les méthodes de l'interface `GradeEngine` sont synchrones.
    - Les contrôleurs JavaFX les appellent exclusivement depuis un thread de travail d'arrière-plan
      (`MaNote-Engine-Worker`).
    - Le moteur n'a donc **pas besoin d'être thread-safe** et ne doit pas manipuler de threads ni de classes JavaFX.
4. **Indépendance totale des bibliothèques d'interface** :
    - Le package `application.engine` et ses sous-packages ne dépendent d' **aucun module JavaFX**.

---

## 2. L'Interface Principale `GradeEngine`

L'interface se situe dans le package `application.engine.GradeEngine`.

### 2.1 Méthodes obligatoires

```java
public interface GradeEngine {

    // Métadonnées
    EngineInfo info();

    EngineCapabilities capabilities();

    // Structure académique (lecture)
    List<AcademicYearSummary> getAcademicYears();

    List<SemesterSummary> getSemesters(AcademicYearId yearId);

    SemesterSummary getSemester(SemesterId semesterId);

    List<UeSummary> getUes(SemesterId semesterId);

    UeDetails getUe(UeId ueId);

    List<EvaluationType> getEvaluationTypes();

    // Évaluations et notes (recherche et CRUD)
    List<EvaluationSummary> getEvaluations(EvaluationFilter filter);

    EvaluationDetails getEvaluation(EvaluationId evaluationId);

    EvaluationId createEvaluation(EvaluationDraft draft);

    void updateEvaluation(EvaluationId evaluationId, EvaluationDraft draft);

    void deleteEvaluation(EvaluationId evaluationId);

    void recordGrade(EvaluationId evaluationId, GradeEntry grade);

    void clearGrade(EvaluationId evaluationId);
}
```

### 2.2 Méthodes facultatives (avec valeurs par défaut)

Si votre moteur ne prend pas encore en charge l'édition de structure, la simulation ou la configuration dynamique, il
n'a rien à coder : ces méthodes lèvent par défaut `OperationNotSupportedException` et l'interface s'adapte
automatiquement grâce à `capabilities()`.

```java
// Édition de structure (si capabilities().structureEditing() == true)
AcademicYearId createAcademicYear(AcademicYearDraft draft);

void updateAcademicYear(AcademicYearId yearId, AcademicYearDraft draft);

SemesterId createSemester(SemesterDraft draft);

void updateSemester(SemesterId semesterId, SemesterDraft draft);

UeId createUe(UeDraft draft);

void updateUe(UeId ueId, UeDraft draft);

void deleteUe(UeId ueId);

// Analyse et simulation (si capabilities().averageTimeline() ou simulation() == true)
List<TimelinePoint> getAverageTimeline(SemesterId semesterId);

SimulationResult simulate(SimulationRequest request);

// Configuration dynamique (si !capabilities().configurationScopes().isEmpty())
ConfigurationView getConfiguration(ConfigurationScope scope);

void updateConfiguration(ConfigurationScope scope, Map<String, ConfigValue> changes);

void resetConfiguration(ConfigurationScope scope);
```

---

## 3. Typage Fort et Objets de Données (DTOs)

Tous les DTOs sont des `records` Java immuables situés dans `application.engine.model.*`.

### 3.1 Notes saisies : `GradeEntry`

Type scellé (`sealed interface`) :

- `GradeEntry.Score(double points, double outOf)` : note chiffrée positive dans son barème.
- `GradeEntry.Absence(boolean justified)` : absence constatée (justifiée ABJ ou injustifiée ABI).

### 3.2 Résultats calculés : `ComputedResult`

Type scellé qui évite toute ambiguïté :

- `ComputedResult.Value(double value, double outOf, ResultNature nature, String explanation)` : note ou moyenne
  calculée, avec sa nature :
    - `ResultNature.FINAL` : moyenne définitive (toutes les notes connues).
    - `ResultNature.PROVISIONAL` : moyenne provisoire basée sur les notes reçues à ce jour.
    - `ResultNature.SIMULATED` : moyenne estimée issue d'une simulation hypothétique.
- `ComputedResult.NotComputable(String reason)` : calcul impossible (ex. « Aucune note disponible »).
- `ComputedResult.NotProvided()` : indicateur non implémenté.
- `ComputedResult.Failed(String message)` : erreur lors du calcul.

### 3.3 Statut d'évaluation : `EvaluationStatus`

- `PLANNED` : évaluation planifiée à venir.
- `AWAITING_GRADE` : évaluation passée, résultat en cours de correction.
- `GRADED` : résultat disponible (possède obligatoirement un `GradeEntry`).
- `CANCELLED` : évaluation annulée.

---

## 4. Gestion des Erreurs et Exceptions

Les exceptions métier se trouvent dans `application.engine.error.*` :

- `NotFoundException` : l'identifiant demandé n'existe pas.
- `ValidationException` : données invalides fournies par l'utilisateur (avec une liste de `FieldError(field, message)`
  pour que l'interface surligne le champ en rouge).
- `OperationNotSupportedException` : fonctionnalité non disponible.
- `EngineException` : toute autre erreur interne ou incohérence. Le message est affiché dans un toast rouge à
  l'utilisateur.

---

## 5. Comment Brancher Votre Moteur Réel

Le chargeur de moteur `infrastructure.engine.EngineLoader` offre trois manières simples et transparentes de connecter
votre moteur :

### Option A — Argument de ligne de commande (Direct)

Vous pouvez passer l'argument direct `--engine=...` au lancement :

```bash
mvn javafx:run -Djavafx.args="--engine=mon.package.metier.MonGradeEngine"
```

Ou avec le binaire compilé :

```bash
java -jar target/MaNote-1.0-SNAPSHOT.jar --engine=mon.package.metier.MonGradeEngine
```

### Option B — Propriété JVM (Idéal pour les scripts et tests)

Passez la propriété système Java indiquant le nom complet de votre classe implémentant `GradeEngine` :

```bash
mvn javafx:run -Dmanote.engine.class=mon.package.metier.MonGradeEngine
```

Le chargeur instanciera automatiquement votre classe via son constructeur public sans argument.

### Option C — Découverte automatique `ServiceLoader` (Recommandé en production)

Créez dans vos ressources le fichier :
`src/main/resources/META-INF/services/application.engine.GradeEngine`
contenant la ligne unique :

```
mon.package.metier.MonGradeEngine
```

Au démarrage, `EngineLoader` découvrira votre moteur automatiquement. Si aucun moteur tiers n'est spécifié,
l'application démarre par défaut sur le moteur relationnel persistant `JpaGradeEngine` (avec la base locale H2), ou sur
`DemoGradeEngine` si `--engine=demo` est explicitement demandé.
