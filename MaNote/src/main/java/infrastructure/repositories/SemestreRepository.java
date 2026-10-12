package infrastructure.repositories;

import domain.entities.Semestre;
import domain.entities.UE;
import infrastructure.persistence.JpaManager;

import java.util.List;
import java.util.Optional;

/**
 * Dépôt JPA pour les Semestres Universitaires.
 */
public class SemestreRepository {

    private final JpaManager jpaManager;

    public SemestreRepository(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    public List<Semestre> findByAnneeId(int anneeId) {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<Semestre> list = em.createQuery(
                            "SELECT DISTINCT s FROM Semestre s LEFT JOIN FETCH s.ues WHERE s.annee.id = :anneeId ORDER BY s.num ASC", Semestre.class)
                    .setParameter("anneeId", anneeId)
                    .getResultList();
            for (Semestre s : list) {
                for (UE u : s.getUes()) {
                    u.getNotes().size();
                }
            }
            return list;
        });
    }

    public Optional<Semestre> findById(int id) {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<Semestre> list = em.createQuery(
                            "SELECT DISTINCT s FROM Semestre s LEFT JOIN FETCH s.ues WHERE s.id = :id", Semestre.class)
                    .setParameter("id", id)
                    .getResultList();
            if (list.isEmpty()) {
                return Optional.empty();
            }
            Semestre s = list.get(0);
            for (UE u : s.getUes()) {
                u.getNotes().size();
            }
            return Optional.of(s);
        });
    }

    public Semestre save(Semestre semestre) {
        return jpaManager.executeInTransactionWithResult(em -> {
            if (semestre.getId() == 0) {
                em.persist(semestre);
                return semestre;
            } else {
                return em.merge(semestre);
            }
        });
    }

    public void delete(int id) {
        jpaManager.executeInTransaction(em -> {
            Semestre s = em.find(Semestre.class, id);
            if (s != null) {
                em.remove(s);
            }
        });
    }
}
