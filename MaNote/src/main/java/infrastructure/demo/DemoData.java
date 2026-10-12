package infrastructure.demo;

import application.engine.model.EvaluationStatus;
import application.engine.model.EvaluationType;
import application.engine.model.GradeEntry;

import java.time.LocalDate;
import java.time.Month;
import java.util.*;

/**
 * État mutable du moteur de démonstration et génération d'un jeu de données fictif mais cohérent.
 * <p>
 * Les dates sont calculées relativement à la date du jour afin que le tableau de bord présente toujours
 * un mélange réaliste de notes disponibles, de notes en attente et d'évaluations à venir.
 * <strong>Toutes ces données sont fictives.</strong>
 */
final class DemoData {

    // ------------------------------------------------------------------ enregistrements internes

    static final class Year {
        int id;
        String label;
        String programme;
        LocalDate start;
        LocalDate end;
    }

    static final class Semester {
        int id;
        int yearId;
        int number;
        String label;
        LocalDate start;
        LocalDate end;
    }

    static final class Ue {
        int id;
        int semesterId;
        String code;
        String name;
        String description = "";
        int credits;
    }

    static final class Evaluation {
        int id;
        int ueId;
        String title;
        String typeCode;
        LocalDate date;            // null = date non fixée
        EvaluationStatus status;
        double maxPoints;
        Double weight;             // null = non renseigné
        String description = "";
        GradeEntry grade;          // non null si et seulement si status == GRADED
        LocalDate gradeRecordedOn;
    }

    // ------------------------------------------------------------------ état

    final Map<Integer, Year> years = new LinkedHashMap<>();
    final Map<Integer, Semester> semesters = new LinkedHashMap<>();
    final Map<Integer, Ue> ues = new LinkedHashMap<>();
    final Map<Integer, Evaluation> evaluations = new LinkedHashMap<>();
    final List<EvaluationType> types = List.of(
            new EvaluationType("DS", "Devoir Surveillé (DS)"),
            new EvaluationType("QUIZ", "Quiz"),
            new EvaluationType("TP", "TP noté"),
            new EvaluationType("CC", "Contrôle continu"),
            new EvaluationType("HOMEWORK", "Devoir maison"),
            new EvaluationType("PROJECT", "Projet"),
            new EvaluationType("ORAL", "Oral / soutenance"),
            new EvaluationType("EXAM", "Examen"),
            new EvaluationType("PARTIEL", "Partiel"),
            new EvaluationType("OTHER", "Autre"));

    private int nextId = 1;

    int nextId() {
        return nextId++;
    }

    // ------------------------------------------------------------------ génération

    /**
     * Modèle d'évaluation : type, intitulé, décalage en jours depuis le début du semestre, barème, coefficient.
     */
    private record T(String type, String title, int day, double max, double weight) {
    }

    /**
     * Construit le jeu de données fictif relativement à {@code today}.
     */
    static DemoData generate(LocalDate today) {
        DemoData d = new DemoData();
        int y = today.getMonthValue() >= Month.SEPTEMBER.getValue() ? today.getYear() : today.getYear() - 1;
        Random random = new Random(42);

        // ---------- Année précédente : Licence 3 (terminée)
        Year l3 = d.year((y - 1) + "–" + y, "Licence 3 Informatique",
                LocalDate.of(y - 1, 9, 1), LocalDate.of(y, 6, 30));
        Semester s5 = d.semester(l3, 5, "Semestre 5", LocalDate.of(y - 1, 9, 1), LocalDate.of(y, 1, 31));
        Semester s6 = d.semester(l3, 6, "Semestre 6", LocalDate.of(y, 2, 1), LocalDate.of(y, 6, 30));

        d.ue(s5, "INF501", "Programmation fonctionnelle", 6, 14.2, generic(), today, random);
        d.ue(s5, "INF502", "Systèmes d'exploitation", 6, 12.4, generic(), today, random);
        d.ue(s5, "INF503", "Théorie des langages", 6, 11.6, generic(), today, random);
        d.ue(s5, "MAT501", "Probabilités et statistiques", 6, 10.8, generic(), today, random);
        d.ue(s5, "LAN501", "Anglais", 6, 15.1, languages(), today, random);

        d.ue(s6, "INF601", "Réseaux", 6, 13.1, generic(), today, random);
        Ue web = d.ue(s6, "INF602", "Programmation web", 6, 15.8, generic(), today, random);
        d.ue(s6, "INF603", "Projet tuteuré", 9, 14.6, project(), today, random);
        d.ue(s6, "INF604", "Logique pour l'informatique", 6, 9.6, generic(), today, random);
        d.ue(s6, "MAT601", "Analyse numérique", 3, 11.9, generic(), today, random);
        // Une absence justifiée pour illustrer la gestion des absences.
        d.evaluations.values().stream()
                .filter(e -> e.ueId == web.id && "QUIZ".equals(e.typeCode))
                .findFirst()
                .ifPresent(e -> {
                    e.grade = GradeEntry.absence(true);
                    e.description = "Absence justifiée (certificat médical).";
                });

        // ---------- Année en cours : Master 1
        Year m1 = d.year(y + "–" + (y + 1), "Master 1 Informatique",
                LocalDate.of(y, 9, 1), LocalDate.of(y + 1, 6, 30));
        Semester s1 = d.semester(m1, 1, "Semestre 1", LocalDate.of(y, 9, 1), LocalDate.of(y + 1, 1, 31));
        Semester s2 = d.semester(m1, 2, "Semestre 2", LocalDate.of(y + 1, 2, 1), LocalDate.of(y + 1, 6, 30));

        d.ue(s1, "INF701", "Algorithmique avancée", 6, 14.1, List.of(
                new T("QUIZ", "Quiz — complexité et récurrences", 12, 10, 0.5),
                new T("TP", "TP noté — parcours de graphes", 23, 20, 1),
                new T("CC", "Contrôle continu 1", 35, 20, 1.5),
                new T("TP", "TP noté — programmation dynamique", 55, 20, 1),
                new T("PROJECT", "Projet — solveur de contraintes", 92, 20, 2),
                new T("EXAM", "Examen terminal", 128, 20, 4)), today, random)
                .description = "Paradigmes algorithmiques avancés : graphes, flots, programmation dynamique, approximation.";
        d.ue(s1, "INF702", "Génie logiciel et architecture", 6, 15.3, List.of(
                new T("HOMEWORK", "Étude de cas — patrons de conception", 16, 20, 1),
                new T("TP", "TP noté — tests et intégration continue", 30, 20, 1),
                new T("PROJECT", "Projet d'architecture — jalon 1", 40, 20, 1.5),
                new T("PROJECT", "Projet d'architecture — livraison finale", 99, 20, 2.5),
                new T("EXAM", "Examen terminal", 130, 20, 3)), today, random)
                .description = "Conception orientée objet, architecture logicielle, qualité et industrialisation.";
        d.ue(s1, "INF703", "Bases de données avancées", 6, 12.2, List.of(
                new T("QUIZ", "Quiz — algèbre relationnelle", 9, 10, 0.5),
                new T("TP", "TP noté — optimisation de requêtes", 33, 20, 1),
                new T("CC", "Contrôle continu — transactions", 41, 20, 1.5),
                new T("TP", "TP noté — NoSQL", 70, 20, 1),
                new T("EXAM", "Examen terminal", 132, 20, 4)), today, random)
                .description = "Optimisation, transactions et concurrence, bases NoSQL et distribuées.";
        d.ue(s1, "INF704", "Réseaux et systèmes distribués", 6, 8.9, List.of(
                new T("TP", "TP noté — sockets", 18, 20, 1),
                new T("CC", "Contrôle continu 1", 28, 20, 1.5),
                new T("QUIZ", "Quiz — consensus", 38, 10, 0.5),
                new T("TP", "TP noté — RPC et microservices", 62, 20, 1),
                new T("CC", "Contrôle continu 2", 84, 20, 1.5),
                new T("EXAM", "Examen terminal", 134, 20, 3.5)), today, random)
                .description = "Protocoles, algorithmes distribués, tolérance aux pannes.";
        d.ue(s1, "INF705", "Apprentissage automatique", 3, 16.2, List.of(
                new T("HOMEWORK", "Devoir — régression et régularisation", 20, 20, 1),
                new T("PROJECT", "Mini-projet — classification d'images", 76, 20, 2),
                new T("ORAL", "Soutenance du mini-projet", 112, 20, 1)), today, random)
                .description = "Apprentissage supervisé, évaluation de modèles, introduction aux réseaux de neurones.";
        d.ue(s1, "LAN701", "Anglais scientifique", 3, 13.4, List.of(
                new T("ORAL", "Présentation orale", 26, 20, 1),
                new T("HOMEWORK", "Rédaction d'un abstract", 57, 20, 1),
                new T("EXAM", "Test écrit final", 121, 20, 2)), today, random);
        // Une évaluation annulée, pour illustrer ce statut.
        d.evaluation(d.findUe("INF703"), "QUIZ", "Quiz — indexation (annulé)", s1.start.plusDays(21), 10, 0.5,
                EvaluationStatus.CANCELLED, null, null).description = "Annulé : séance non assurée.";

        d.ue(s2, "INF801", "Compilation", 6, 13.0, generic(), today, random);
        d.ue(s2, "INF802", "Sécurité informatique", 6, 13.0, generic(), today, random);
        d.ue(s2, "INF803", "Intelligence artificielle", 6, 13.0, generic(), today, random);
        d.ue(s2, "INF804", "Projet de développement", 6, 13.0, project(), today, random);
        d.ue(s2, "INF805", "Cloud et DevOps", 3, 13.0, languages(), today, random);
        // Une évaluation dont la date n'est pas encore fixée.
        d.evaluation(d.findUe("INF804"), "ORAL", "Soutenance finale du projet", null, 20, 2,
                EvaluationStatus.PLANNED, null, null).description = "Date communiquée ultérieurement.";
        return d;
    }

    private static List<T> generic() {
        return List.of(
                new T("QUIZ", "Quiz 1", 14, 10, 0.5),
                new T("TP", "TP noté 1", 30, 20, 1),
                new T("CC", "Contrôle continu", 50, 20, 1.5),
                new T("TP", "TP noté 2", 78, 20, 1),
                new T("EXAM", "Examen terminal", 128, 20, 4));
    }

    private static List<T> project() {
        return List.of(
                new T("PROJECT", "Cahier des charges", 21, 20, 1),
                new T("PROJECT", "Revue intermédiaire", 63, 20, 1.5),
                new T("PROJECT", "Rapport final", 112, 20, 2.5),
                new T("ORAL", "Soutenance", 125, 20, 2));
    }

    private static List<T> languages() {
        return List.of(
                new T("ORAL", "Oral", 35, 20, 1),
                new T("HOMEWORK", "Devoir écrit", 70, 20, 1),
                new T("EXAM", "Examen final", 120, 20, 2));
    }

    // ------------------------------------------------------------------ fabriques élémentaires

    Year year(String label, String programme, LocalDate start, LocalDate end) {
        Year year = new Year();
        year.id = nextId();
        year.label = label;
        year.programme = programme;
        year.start = start;
        year.end = end;
        years.put(year.id, year);
        return year;
    }

    Semester semester(Year year, int number, String label, LocalDate start, LocalDate end) {
        Semester s = new Semester();
        s.id = nextId();
        s.yearId = year.id;
        s.number = number;
        s.label = label;
        s.start = start;
        s.end = end;
        semesters.put(s.id, s);
        return s;
    }

    private Ue ue(Semester s, String code, String name, int credits, double level, List<T> templates,
                  LocalDate today, Random random) {
        Ue ue = new Ue();
        ue.id = nextId();
        ue.semesterId = s.id;
        ue.code = code;
        ue.name = name;
        ue.credits = credits;
        ues.put(ue.id, ue);
        int shift = (ues.size() * 3) % 7; // étale les échéances entre UE
        for (T t : templates) {
            LocalDate date = s.start.plusDays(t.day + shift);
            if (date.isAfter(s.end)) {
                date = s.end.minusDays(3);
            }
            EvaluationStatus status;
            GradeEntry grade = null;
            LocalDate recorded = null;
            if (!date.isBefore(today)) {
                status = EvaluationStatus.PLANNED;
            } else if (date.isAfter(today.minusDays(11))) {
                status = EvaluationStatus.AWAITING_GRADE;
            } else {
                status = EvaluationStatus.GRADED;
                double noise = random.nextGaussian() * 2.2;
                double on20 = Math.max(0, Math.min(20, level + noise));
                double points = Math.round(on20 / 20 * t.max * 4) / 4.0;
                grade = GradeEntry.score(points, t.max);
                recorded = date.plusDays(6 + random.nextInt(4));
                if (recorded.isAfter(today)) recorded = today;
            }
            evaluation(ue, t.type, t.title, date, t.max, t.weight, status, grade, recorded);
        }
        return ue;
    }

    Evaluation evaluation(Ue ue, String type, String title, LocalDate date, double max, double weight,
                          EvaluationStatus status, GradeEntry grade, LocalDate recordedOn) {
        Evaluation e = new Evaluation();
        e.id = nextId();
        e.ueId = ue.id;
        e.title = title;
        e.typeCode = type;
        e.date = date;
        e.maxPoints = max;
        e.weight = weight;
        e.status = status;
        e.grade = grade;
        e.gradeRecordedOn = recordedOn;
        evaluations.put(e.id, e);
        return e;
    }

    Ue findUe(String code) {
        return ues.values().stream().filter(u -> u.code.equals(code)).reduce((a, b) -> b).orElseThrow();
    }

    List<Evaluation> evaluationsOf(int ueId) {
        List<Evaluation> list = new ArrayList<>();
        for (Evaluation e : evaluations.values()) {
            if (e.ueId == ueId) list.add(e);
        }
        return list;
    }

    List<Ue> uesOf(int semesterId) {
        List<Ue> list = new ArrayList<>();
        for (Ue u : ues.values()) {
            if (u.semesterId == semesterId) list.add(u);
        }
        return list;
    }

    List<Semester> semestersOf(int yearId) {
        List<Semester> list = new ArrayList<>();
        for (Semester s : semesters.values()) {
            if (s.yearId == yearId) list.add(s);
        }
        list.sort((a, b) -> Integer.compare(a.number, b.number));
        return list;
    }
}
