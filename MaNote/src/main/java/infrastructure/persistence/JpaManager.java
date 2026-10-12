package infrastructure.persistence;

import domain.entities.Annee;
import domain.entities.Note;
import domain.entities.Semestre;
import domain.entities.UE;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Gestionnaire central de la persistance JPA avec Hibernate et base H2.
 */
public final class JpaManager {

    private static volatile JpaManager instance;
    private final EntityManagerFactory emf;
    private final String databaseUrl;
    private final String databasePath;

    private JpaManager(String dbUrl, String dbPath) {
        this.databaseUrl = dbUrl;
        this.databasePath = dbPath;
        this.emf = createEntityManagerFactory(dbUrl);
    }

    public static JpaManager getInstance() {
        if (instance == null || (instance.emf != null && !instance.emf.isOpen())) {
            synchronized (JpaManager.class) {
                if (instance == null || (instance.emf != null && !instance.emf.isOpen())) {
                    instance = createDefault();
                }
            }
        }
        return instance;
    }

    private static JpaManager createDefault() {
        String customUrl = System.getProperty("manote.db.url");
        if (customUrl != null && !customUrl.isBlank()) {
            return new JpaManager(customUrl.trim(), "personnalisé");
        }

        File dataDir = new File(System.getProperty("user.home"), ".manote");
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
        File dbFile = new File(dataDir, "manote");
        String url = "jdbc:h2:file:" + dbFile.getAbsolutePath() + ";AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";
        return new JpaManager(url, dbFile.getAbsolutePath() + ".mv.db");
    }

    public static synchronized void resetForTesting(String inMemoryDbName) {
        if (instance != null) {
            instance.close();
        }
        String url = "jdbc:h2:mem:" + inMemoryDbName + ";DB_CLOSE_DELAY=-1";
        instance = new JpaManager(url, ":memory:");
    }

    private EntityManagerFactory createEntityManagerFactory(String jdbcUrl) {
        Map<String, Object> settings = new HashMap<>();
        settings.put(Environment.JAKARTA_JDBC_DRIVER, "org.h2.Driver");
        settings.put(Environment.JAKARTA_JDBC_URL, jdbcUrl);
        settings.put(Environment.JAKARTA_JDBC_USER, "sa");
        settings.put(Environment.JAKARTA_JDBC_PASSWORD, "");
        settings.put(Environment.DIALECT, "org.hibernate.dialect.H2Dialect");
        settings.put(Environment.HBM2DDL_AUTO, "update");
        settings.put(Environment.SHOW_SQL, "false");
        settings.put(Environment.FORMAT_SQL, "false");

        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySettings(settings)
                .build();

        MetadataSources sources = new MetadataSources(registry);
        sources.addAnnotatedClass(Annee.class);
        sources.addAnnotatedClass(Semestre.class);
        sources.addAnnotatedClass(UE.class);
        sources.addAnnotatedClass(Note.class);

        Metadata metadata = sources.getMetadataBuilder().build();
        return metadata.getSessionFactoryBuilder().build();
    }

    public EntityManager createEntityManager() {
        return emf.createEntityManager();
    }

    public void executeInTransaction(Consumer<EntityManager> action) {
        EntityManager em = createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            action.accept(em);
            tx.commit();
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public <R> R executeInTransactionWithResult(Function<EntityManager, R> action) {
        EntityManager em = createEntityManager();
        EntityTransaction tx = em.getTransaction();
        try {
            tx.begin();
            R result = action.apply(em);
            tx.commit();
            return result;
        } catch (RuntimeException e) {
            if (tx != null && tx.isActive()) {
                tx.rollback();
            }
            throw e;
        } finally {
            em.close();
        }
    }

    public String getDatabasePath() {
        return databasePath;
    }

    public String getDatabaseUrl() {
        return databaseUrl;
    }

    public synchronized void close() {
        if (emf != null && emf.isOpen()) {
            emf.close();
        }
        if (instance == this) {
            instance = null;
        }
    }
}
