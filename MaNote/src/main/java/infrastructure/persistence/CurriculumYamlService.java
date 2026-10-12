package infrastructure.persistence;

import domain.entities.Annee;
import domain.entities.Note;
import domain.entities.Semestre;
import domain.entities.UE;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * Service de synchronisation et de gestion du fichier de configuration YAML du cursus (~/.manote/cursus.yaml).
 * Permet à l'étudiant de définir ses Années, Semestres, UE et DS/Évaluations avec leurs pourcentages
 * soit via un fichier éditable, soit via l'interface graphique.
 */
public class CurriculumYamlService {

    private final JpaManager jpaManager;
    private final File dataDir;
    private final File yamlFile;
    private final File syncTimestampFile;

    public CurriculumYamlService(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
        this.dataDir = new File(System.getProperty("user.home"), ".manote");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        this.yamlFile = new File(dataDir, "cursus.yaml");
        this.syncTimestampFile = new File(dataDir, ".cursus.yaml.sync");
    }

    public File getYamlFile() {
        return yamlFile;
    }

    public boolean yamlFileExists() {
        return yamlFile.exists() && yamlFile.length() > 0;
    }

    /**
     * Crée un fichier modèle template si aucun fichier n'existe, pour guider l'utilisateur.
     */
    public void createTemplateFileIfNotExists() {
        File templateFile = new File(dataDir, "cursus.yaml.template");
        if (!templateFile.exists()) {
            String templateContent = """
                    # ==============================================================================
                    # MaNote — Exemple de configuration de cursus (cursus.yaml.template)
                    #
                    # Pour utiliser ce fichier :
                    # 1. Renommez-le en 'cursus.yaml' dans le dossier ~/.manote/
                    # 2. Modifiez vos UE, vos DS et leurs pourcentages / coefficients
                    # 3. Lancez l'application MaNote ou cliquez sur 'Recharger YAML'
                    # ==============================================================================
                    
                    annee: "Master 1 Informatique (2025-2026)"
                    semestres:
                      - numero: 7
                        nom: "Semestre 7"
                        ues:
                          - code: "INFO-M1-CL"
                            nom: "Conception Logicielle"
                            credits: 6
                            evaluations:
                              - titre: "DS 1 — Conception"
                                type: "DS"
                                pourcentage: 40
                                bareme: 20
                              - titre: "Projet Application"
                                type: "PROJET"
                                pourcentage: 60
                                bareme: 20
                    
                          - code: "INFO-M1-SYS"
                            nom: "Systèmes Répartis"
                            credits: 6
                            evaluations:
                              - titre: "DS 1"
                                type: "DS"
                                pourcentage: 50
                                bareme: 20
                              - titre: "TP noté"
                                type: "TP"
                                pourcentage: 50
                                bareme: 20
                    """;
            try {
                Files.writeString(templateFile.toPath(), templateContent, StandardCharsets.UTF_8);
            } catch (IOException ignored) {
            }
        }
    }

    /**
     * Vérifie si le fichier YAML a été modifié depuis la dernière synchronisation.
     */
    public boolean isYamlModifiedSinceLastSync() {
        if (!yamlFileExists()) {
            return false;
        }
        if (!syncTimestampFile.exists()) {
            return true;
        }
        try {
            long lastSync = Long.parseLong(Files.readString(syncTimestampFile.toPath(), StandardCharsets.UTF_8).trim());
            return yamlFile.lastModified() > lastSync;
        } catch (Exception e) {
            return true;
        }
    }

    private void markSynced() {
        try {
            Files.writeString(syncTimestampFile.toPath(), String.valueOf(yamlFile.lastModified()), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
        }
    }

    /**
     * Synchronise le cursus depuis le fichier YAML si celui-ci existe et a été modifié.
     *
     * @return true si une synchronisation a eu lieu
     */
    public boolean syncIfModified() {
        if (yamlFileExists() && isYamlModifiedSinceLastSync()) {
            return importFromYaml(yamlFile);
        }
        return false;
    }

    /**
     * Importe la structure complète depuis un fichier YAML et l'enregistre dans la base JPA.
     */
    @SuppressWarnings("unchecked")
    public boolean importFromYaml(File file) {
        if (!file.exists()) return false;

        Yaml yaml = new Yaml();
        Map<String, Object> root;
        try (InputStream is = new FileInputStream(file)) {
            Object loaded = yaml.load(is);
            if (!(loaded instanceof Map<?, ?> map)) {
                return false;
            }
            root = (Map<String, Object>) map;
        } catch (Exception e) {
            System.err.println("[CurriculumYamlService] Erreur de lecture YAML : " + e.getMessage());
            return false;
        }

        String anneeNom = (String) root.getOrDefault("annee", "Année Universitaire");
        List<Map<String, Object>> semestresList = (List<Map<String, Object>>) root.get("semestres");

        jpaManager.executeInTransaction(em -> {
            // Rechercher ou créer l'année
            List<Annee> existingAnnees = em.createQuery("SELECT a FROM Annee a WHERE a.nom = :nom", Annee.class)
                    .setParameter("nom", anneeNom)
                    .getResultList();

            Annee annee;
            if (!existingAnnees.isEmpty()) {
                annee = existingAnnees.get(0);
            } else {
                annee = new Annee(anneeNom);
                em.persist(annee);
            }

            if (semestresList == null) return;

            for (Map<String, Object> semMap : semestresList) {
                Object numObj = semMap.get("numero");
                char numChar = '1';
                if (numObj instanceof Number n) {
                    numChar = String.valueOf(n.intValue()).charAt(0);
                } else if (numObj instanceof String s && !s.isBlank()) {
                    numChar = s.charAt(0);
                }

                String semNom = (String) semMap.getOrDefault("nom", "Semestre " + numChar);

                // Rechercher ou créer le semestre dans l'année
                Semestre semestre = null;
                if (annee.getSemestres() != null) {
                    for (Semestre s : annee.getSemestres()) {
                        if (s.getNum() == numChar || s.getNom().equalsIgnoreCase(semNom)) {
                            semestre = s;
                            break;
                        }
                    }
                }

                if (semestre == null) {
                    semestre = new Semestre(numChar, semNom);
                    annee.addSemestre(semestre);
                    em.persist(semestre);
                } else {
                    semestre.setNom(semNom);
                }

                List<Map<String, Object>> uesList = (List<Map<String, Object>>) semMap.get("ues");
                if (uesList == null) continue;

                for (Map<String, Object> ueMap : uesList) {
                    String code = (String) ueMap.getOrDefault("code", "UE");
                    String nom = (String) ueMap.getOrDefault("nom", code);
                    int credits = 6;
                    if (ueMap.get("credits") instanceof Number n) {
                        credits = n.intValue();
                    } else if (ueMap.get("credit") instanceof Number n) {
                        credits = n.intValue();
                    }
                    String desc = (String) ueMap.getOrDefault("description", "");

                    // Rechercher ou créer l'UE
                    UE ue = null;
                    if (semestre.getUes() != null) {
                        for (UE u : semestre.getUes()) {
                            if (u.getCode().equalsIgnoreCase(code)) {
                                ue = u;
                                break;
                            }
                        }
                    }

                    if (ue == null) {
                        ue = new UE(nom, code, credits);
                        ue.setDescription(desc);
                        semestre.addUe(ue);
                        em.persist(ue);
                    } else {
                        ue.setNom(nom);
                        ue.setCredit(credits);
                        if (desc != null && !desc.isBlank()) ue.setDescription(desc);
                    }

                    List<Map<String, Object>> evalsList = (List<Map<String, Object>>) ueMap.get("evaluations");
                    if (evalsList == null) continue;

                    for (Map<String, Object> evalMap : evalsList) {
                        String titre = (String) evalMap.getOrDefault("titre", "Évaluation");
                        String type = (String) evalMap.getOrDefault("type", "DS");

                        double bareme = 20.0;
                        if (evalMap.get("bareme") instanceof Number n) {
                            bareme = n.doubleValue();
                        }

                        double poids = 1.0;
                        if (evalMap.get("pourcentage") instanceof Number n) {
                            double p = n.doubleValue();
                            poids = p > 1.0 ? p / 100.0 : p;
                        } else if (evalMap.get("coefficient") instanceof Number n) {
                            poids = n.doubleValue();
                        } else if (evalMap.get("poids") instanceof Number n) {
                            poids = n.doubleValue();
                        }

                        // Rechercher ou créer la Note
                        Note note = null;
                        if (ue.getNotes() != null) {
                            for (Note nt : ue.getNotes()) {
                                if (nt.getNom().equalsIgnoreCase(titre)) {
                                    note = nt;
                                    break;
                                }
                            }
                        }

                        if (note == null) {
                            note = new Note(titre, type, new Date(), bareme, poids);
                            note.setStatus("PLANNED");
                            ue.addNote(note);
                            em.persist(note);
                        } else {
                            note.setType(type);
                            note.setBareme(bareme);
                            note.setCoefficient(poids);
                        }

                        // Valeur éventuelle si déjà renseignée
                        if (evalMap.get("note") instanceof Number n) {
                            note.setValeur(n.doubleValue());
                        }
                    }
                }
            }
        });

        markSynced();
        return true;
    }

    /**
     * Exporte la structure actuelle de la base de données vers le fichier YAML.
     */
    public boolean exportToYaml(File file) {
        List<Annee> annees = jpaManager.executeInTransactionWithResult(em -> {
            return em.createQuery("SELECT DISTINCT a FROM Annee a LEFT JOIN FETCH a.semestres ORDER BY a.id ASC", Annee.class)
                    .getResultList();
        });

        if (annees.isEmpty()) {
            return false;
        }

        Annee activeYear = annees.get(annees.size() - 1);

        Map<String, Object> root = new LinkedHashMap<>();
        root.put("annee", activeYear.getNom());

        List<Map<String, Object>> semestresList = new ArrayList<>();
        if (activeYear.getSemestres() != null) {
            for (Semestre s : activeYear.getSemestres()) {
                Map<String, Object> semMap = new LinkedHashMap<>();
                semMap.put("numero", Character.getNumericValue(s.getNum()));
                semMap.put("nom", s.getNom());

                List<Map<String, Object>> uesList = new ArrayList<>();
                if (s.getUes() != null) {
                    for (UE u : s.getUes()) {
                        Map<String, Object> ueMap = new LinkedHashMap<>();
                        ueMap.put("code", u.getCode());
                        ueMap.put("nom", u.getNom());
                        ueMap.put("credits", u.getCredit());
                        if (u.getDescription() != null && !u.getDescription().isBlank()) {
                            ueMap.put("description", u.getDescription());
                        }

                        List<Map<String, Object>> evalsList = new ArrayList<>();
                        if (u.getNotes() != null) {
                            for (Note n : u.getNotes()) {
                                Map<String, Object> evMap = new LinkedHashMap<>();
                                evMap.put("titre", n.getNom());
                                evMap.put("type", n.getType() != null ? n.getType() : "DS");
                                evMap.put("pourcentage", Math.round(n.getCoefficient() * 100));
                                evMap.put("bareme", (int) n.getBareme());
                                if (n.getValeur() != null) {
                                    evMap.put("note", n.getValeur());
                                }
                                evalsList.add(evMap);
                            }
                        }
                        ueMap.put("evaluations", evalsList);
                        uesList.add(ueMap);
                    }
                }
                semMap.put("ues", uesList);
                semestresList.add(semMap);
            }
        }
        root.put("semestres", semestresList);

        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);
        Yaml yaml = new Yaml(options);

        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write("# ==============================================================================\n");
            writer.write("# MaNote — Configuration du Cursus Académique (" + file.getName() + ")\n");
            writer.write("# Ce fichier est synchronisé avec l'application. Vous pouvez le modifier librement.\n");
            writer.write("# ==============================================================================\n\n");
            yaml.dump(root, writer);
            markSynced();
            return true;
        } catch (IOException e) {
            System.err.println("[CurriculumYamlService] Erreur d'écriture YAML : " + e.getMessage());
            return false;
        }
    }
}
