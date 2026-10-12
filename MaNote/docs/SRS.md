# Spécification des Exigences Logicielles (Software Requirements Specification — SRS)

> **Projet** : MaNote — Application Desktop de Gestion des Notes Universitaires  
> **Public cible** : Étudiants en Master 1 Informatique et formation universitaire  
> **Version du document** : 1.0 (Octobre 2026)

---

## 1. Introduction et Objectifs

MaNote est une application de bureau haut de gamme conçue en Java permettant à un étudiant universitaire de centraliser,
suivre et analyser ses notes, ses unités d'enseignement (UE), ses semestres et ses évaluations prévues ou effectuées.

L'application établit une séparation étanche entre :

- La **présentation ergonomique moderne** (UI desktop sous JavaFX, design system, graphiques, navigation) ;
- Le **moteur métier de calcul** (développé par l'étudiant, responsable exclusif des formules universitaires).

---

## 2. Périmètre et Acteurs

- **Étudiant (Utilisateur final)** :
    - Consulte son tableau de bord, ses UE et ses moyennes calculées par le moteur.
    - Planifie des évaluations futures avant même d'obtenir une note.
    - Saisit ses notes obtenues ou signale ses absences.
    - Explore l'historique de ses semestres et années antérieures.
    - Effectue des simulations indicatives de ses futures moyennes.
    - Configure les paramètres de calcul exposés par le moteur.

---

## 3. Exigences Fonctionnelles (Functional Requirements)

### 3.1 Structure Académique & Navigation

- **`FR-001`** : L'application doit afficher la hiérarchie académique complète (Année universitaire → Semestres → UE →
  Évaluations).
- **`FR-002`** : L'utilisateur doit pouvoir basculer entre les semestres via un sélecteur persistant situé dans la barre
  supérieure.
- **`FR-003`** : L'application doit conserver un historique clair des semestres précédents sans mélanger leurs données
  avec celles du semestre courant.

### 3.2 Tableau de Bord (Dashboard)

- **`FR-004`** : L'écran d'accueil doit afficher la moyenne générale semestrielle issue du moteur, en distinguant
  explicitement les résultats définitifs (`FINAL`), provisoires (`PROVISIONAL`) ou simulés (`SIMULATED`).
- **`FR-005`** : Le tableau de bord doit présenter les 5 dernières notes enregistrées et les prochaines échéances à
  venir.
- **`FR-006`** : Une grille interactive des UE doit afficher pour chacune son code, ses crédits ECTS, sa moyenne et sa
  barre de complétion.

### 3.3 Gestion des Évaluations & Traitement des Notes

- **`FR-007`** : L'application doit distinguer 4 statuts d'évaluation : Planifiée (`PLANNED`), Réalisée en attente de
  note (`AWAITING_GRADE`), Notée (`GRADED`) et Annulée (`CANCELLED`).
- **`FR-008`** : **Règle impérative** : Une note absente ou en attente ne doit **jamais** être assimilée à zéro dans
  l'interface ou les modèles.
- **`FR-009`** : L'utilisateur peut créer une évaluation sans note, avec titre, type (CC, TP, Projet, Examen…), date
  prévisionnelle, barème (ex. /20, /40) et coefficient.
- **`FR-010`** : La saisie d'une note permet d'enregistrer des points dans le barème ou de déclarer une absence
  justifiée (ABJ) ou injustifiée (ABI).
- **`FR-011`** : L'application doit valider que la note saisie est positive et ne dépasse pas la note maximale de
  l'évaluation.
- **`FR-012`** : L'utilisateur peut retirer une note enregistrée, ramenant automatiquement l'évaluation en statut « Note
  en attente ».

### 3.4 Analyse, Graphiques & Simulations

- **`FR-013`** : L'écran d'analyse doit présenter un graphique comparatif des moyennes par UE par rapport au seuil de
  validation.
- **`FR-014`** : L'écran d'analyse doit afficher l'évolution chronologique de la moyenne au fil de la parution des notes
  (si supporté par le moteur).
- **`FR-015`** : Un simulateur interactif doit permettre de tester des hypothèses de notes sur les évaluations sans
  note, avec affichage explicite de la mention `SIMULATED`.

### 3.5 Planification

- **`FR-016`** : L'écran de planification doit regrouper les évaluations à venir par horizons temporels (semaine
  imminente, mois, fin de semestre, date indéterminée) pour identifier les semaines chargées.

### 3.6 Paramétrage du Moteur & Préférences

- **`FR-017`** : L'application doit générer dynamiquement l'écran de paramétrage à partir du contrat `ConfigurationView`
  auto-décrit par le moteur (seuils, gestion des absences, compensations).
- **`FR-018`** : L'utilisateur doit pouvoir basculer entre le thème clair et le thème sombre à tout moment, le choix
  étant persisté localement.
- **`FR-019`** : L'utilisateur peut activer ou désactiver la demande de confirmation lors de la suppression d'une
  évaluation.

---

## 4. Exigences Non Fonctionnelles

- **`NFR-001 (Performance & Fluidité)`** : Le thread graphique JavaFX ne doit jamais être bloqué. Tous les appels vers
  le moteur métier transitent par un thread de travail dédié (`ExecutorService`).
- **`NFR-002 (Inversion de Dépendance)`** : L'application communique exclusivement via l'interface Java `GradeEngine`.
  Elle doit pouvoir basculer du moteur de démonstration au moteur réel sans aucune modification de l'interface
  graphique.
- **`NFR-003 (Ergonomie Desktop)`** : La fenêtre doit être redimensionnable (minimum 960×640) sans rupture d'alignement
  ou de disposition.
- **`NFR-004 (Portabilité)`** : Compatible macOS, Windows et Linux avec Java 25 et OpenJFX 25.
- **`NFR-005 (Qualité du Code & Automatisation)`** : Préservation du code existant, couverture par 20 tests unitaires
  JUnit 5, respect des conventions de nommage, automatisation via `Makefile` et documentation JavaDoc complète.
