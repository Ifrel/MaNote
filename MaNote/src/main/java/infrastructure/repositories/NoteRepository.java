package infrastructure.repositories;

import domain.entities.Note;
import infrastructure.persistence.JpaManager;

import java.util.List;
import java.util.Optional;

/**
 * Dépôt JPA pour les Notes / Évaluations.
 */
public class NoteRepository {

    private final JpaManager jpaManager;

    public NoteRepository(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    public List<Note> findByUeId(int ueId) {
        return jpaManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT n FROM Note n LEFT JOIN FETCH n.ue u LEFT JOIN FETCH u.semestre WHERE n.ue.id = :ueId ORDER BY n.date ASC, n.id ASC", Note.class)
                        .setParameter("ueId", ueId)
                        .getResultList()
        );
    }

    public List<Note> findBySemestreId(int semestreId) {
        return jpaManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT n FROM Note n LEFT JOIN FETCH n.ue u LEFT JOIN FETCH u.semestre WHERE u.semestre.id = :semId ORDER BY n.date ASC, n.id ASC", Note.class)
                        .setParameter("semId", semestreId)
                        .getResultList()
        );
    }

    public List<Note> findAll() {
        return jpaManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT n FROM Note n LEFT JOIN FETCH n.ue u LEFT JOIN FETCH u.semestre ORDER BY n.date ASC, n.id ASC", Note.class)
                        .getResultList()
        );
    }

    public Optional<Note> findById(int id) {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<Note> list = em.createQuery(
                            "SELECT n FROM Note n LEFT JOIN FETCH n.ue u LEFT JOIN FETCH u.semestre WHERE n.id = :id", Note.class)
                    .setParameter("id", id)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        });
    }

    public Note save(Note note) {
        return jpaManager.executeInTransactionWithResult(em -> {
            if (note.getId() == 0) {
                em.persist(note);
                return note;
            } else {
                return em.merge(note);
            }
        });
    }

    public void delete(int id) {
        jpaManager.executeInTransaction(em -> {
            Note n = em.find(Note.class, id);
            if (n != null) {
                em.remove(n);
            }
        });
    }
}
