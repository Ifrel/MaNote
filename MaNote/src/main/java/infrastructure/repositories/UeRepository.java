package infrastructure.repositories;

import domain.entities.UE;
import infrastructure.persistence.JpaManager;

import java.util.List;
import java.util.Optional;

/**
 * Dépôt JPA pour les Unités d'Enseignement (UE).
 */
public class UeRepository {

    private final JpaManager jpaManager;

    public UeRepository(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    public List<UE> findBySemestreId(int semestreId) {
        return jpaManager.executeInTransactionWithResult(em ->
                em.createQuery("SELECT DISTINCT u FROM UE u LEFT JOIN FETCH u.notes LEFT JOIN FETCH u.semestre WHERE u.semestre.id = :semId ORDER BY u.code ASC", UE.class)
                        .setParameter("semId", semestreId)
                        .getResultList()
        );
    }

    public Optional<UE> findById(int id) {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<UE> list = em.createQuery(
                            "SELECT DISTINCT u FROM UE u LEFT JOIN FETCH u.notes LEFT JOIN FETCH u.semestre WHERE u.id = :id", UE.class)
                    .setParameter("id", id)
                    .getResultList();
            return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
        });
    }

    public UE save(UE ue) {
        return jpaManager.executeInTransactionWithResult(em -> {
            if (ue.getId() == 0) {
                em.persist(ue);
                return ue;
            } else {
                return em.merge(ue);
            }
        });
    }

    public void delete(int id) {
        jpaManager.executeInTransaction(em -> {
            UE ue = em.find(UE.class, id);
            if (ue != null) {
                em.remove(ue);
            }
        });
    }
}
