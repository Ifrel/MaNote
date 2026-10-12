# MaNote — Système de Design Graphique (UI Design System)

> Spécification du design system desktop moderne de l'application MaNote.
> Inspiré des standards visuels contemporains (Flutter desktop, Linear, Raycast).

---

## 1. Principes Fondamentaux de la Direction Artistique

- **Clarté et Hiérarchie** : Une typographie soignée et des contrastes équilibrés mettent en valeur l'information
  académique clé (moyenne, statut, échéance) sans surcharge décorative.
- **Cartes Élégantes et Subtiles** : Surfaces blanches (ou ardoise sombre) avec bordures ultra-fines (`1px`, Slate-200 /
  Slate-700) et ombres gaussiennes douces.
- **Colorimétrie Sémantique Précise** : Couleurs dédiées aux performances et statuts universitaires (vert menthe pour
  validé/noté, ambre pour attention/en attente, rouge doux pour ajourné, bleu pour planifié).
- **Thèmes Dynamiques** : Thème Clair (`variables-light.css`) et Thème Sombre (`variables-dark.css`) interchangeables à
  chaud sans redémarrage de l'application.

---

## 2. Palette de Couleurs & Tokens CSS

| Token CSS            | Thème Clair            | Thème Sombre           | Usage                                        |
|----------------------|------------------------|------------------------|----------------------------------------------|
| `-fx-bg-app`         | `#F8FAFC` (Slate 50)   | `#0F172A` (Slate 900)  | Fond général de la fenêtre                   |
| `-fx-bg-surface`     | `#FFFFFF`              | `#1E293B` (Slate 800)  | Fond des cartes et conteneurs                |
| `-fx-bg-sidebar`     | `#FFFFFF`              | `#1E293B`              | Fond de la barre latérale                    |
| `-fx-border-subtle`  | `#E2E8F0` (Slate 200)  | `#334155` (Slate 700)  | Bordures délicates des cartes et séparateurs |
| `-fx-text-primary`   | `#0F172A` (Slate 900)  | `#F8FAFC` (Slate 50)   | Titres et textes principaux                  |
| `-fx-text-secondary` | `#475569` (Slate 600)  | `#94A3B8` (Slate 400)  | Sous-titres et libellés                      |
| `-fx-text-tertiary`  | `#94A3B8` (Slate 400)  | `#64748B` (Slate 500)  | Dates, indications discrètes                 |
| `-fx-primary-600`    | `#4F46E5` (Indigo 600) | `#818CF8` (Indigo 400) | Couleur d'accent et boutons d'action         |
| `-fx-success-solid`  | `#10B981` (Emeraude)   | `#10B981`              | Validation, notes acquises                   |
| `-fx-warning-solid`  | `#F59E0B` (Ambre)      | `#F59E0B`              | Notes en attente, UE à surveiller            |
| `-fx-danger-solid`   | `#EF4444` (Rouge)      | `#EF4444`              | Non validé, suppression                      |
| `-fx-info-solid`     | `#3B82F6` (Bleu)       | `#3B82F6`              | Évaluations planifiées                       |

---

## 3. Typographie

L'application intègre la police variable open source **Inter (OFL)** embarquée dans `src/main/resources/fonts/` pour
garantir un rendu parfait et strictement identique sur macOS, Linux et Windows.

- **Titres Principaux (`.heading-xl`)** : 24px, Bold.
- **Titres de Cartes / Sections (`.heading-lg`, `.heading-md`)** : 18px / 15px, Semi-Bold.
- **Sous-titres & En-têtes de colonnes (`.heading-sm`)** : 13px, Semi-Bold.
- **Valeurs Statistiques (`.stat-value`)** : 26px, Bold.
- **Corps de texte** : 13px, Regular.

---

## 4. Bibliothèque de Composants Graphiques

| Composant            | Fichier source                         | Caractéristiques                                                                                        |
|----------------------|----------------------------------------|---------------------------------------------------------------------------------------------------------|
| `AppShell`           | `api.views.app.AppShell`               | Structure globale unifiée, zone de notification toast flottante, transition d'écran animée.             |
| `SidebarNavigation`  | `api.views.widgets.SidebarNavigation`  | Logo stylisé, version, boutons avec icônes Feather, mise en évidence de l'écran actif.                  |
| `TopBar`             | `api.views.widgets.TopBar`             | Fil d'ariane, sélecteur rapide de semestre, bascule thème Moon/Sun, badge mode démo.                    |
| `StatCard`           | `api.views.widgets.StatCard`           | Carte KPI avec chiffre mis en avant, icône vectorielle, sous-titre et badge.                            |
| `UECard`             | `api.views.widgets.UECard`             | Carte d'UE interactive, moyenne calculée, crédits, barre de progression des notes reçues.               |
| `StatusBadge`        | `api.views.widgets.StatusBadge`        | Badges à coins arrondis (`20px`) avec styles sémantiques (succès, avertissement, danger, info, neutre). |
| `EmptyState`         | `api.views.widgets.EmptyState`         | Présentation soignée en cas d'absence de données (icône 48px, texte explicatif, bouton d'action).       |
| `EvaluationDialog`   | `api.views.dialogs.EvaluationDialog`   | Formulaire modal complet avec validation des champs.                                                    |
| `GradeDialog`        | `api.views.dialogs.GradeDialog`        | Saisie de note chiffrée sur barème, déclaration d'absence (justifiée/injustifiée) ou retrait.           |
| `ConfirmationDialog` | `api.views.dialogs.ConfirmationDialog` | Boîte de dialogue de confirmation pour les opérations destructives.                                     |

---

## 5. Ergonomie Desktop & Accessibilité

- **Redimensionnement dynamique** : Tous les conteneurs utilisent `ScrollPane`, `FlowPane` et des politiques de
  redimensionnement de tableau pour s'adapter à toutes les résolutions d'écran (minimum 960×640, recommandé 1200×780 ou
  plein écran).
- **Tooltips d'aide** : Présents sur toutes les icônes d'action (éditer, saisir note, supprimer).
- **Mémorisation des préférences** : Le choix du thème et le dernier semestre ouvert sont mémorisés de façon persistante
  via `java.util.prefs.Preferences`.
