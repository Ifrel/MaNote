package infrastructure.repositories;

import domain.entities.Annee;
import domain.entities.Semestre;
import domain.entities.UE;
import infrastructure.persistence.JpaManager;

import java.util.List;
import java.util.Optional;

/**
 * Dépôt JPA pour les Années Universitaires.
 */
public class AnneeRepository {

    private final JpaManager jpaManager;

    public AnneeRepository(JpaManager jpaManager) {
        this.jpaManager = jpaManager;
    }

    public List<Annee> findAll() {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<Annee> list = em.createQuery("SELECT DISTINCT a FROM Annee a LEFT JOIN FETCH a.semestres ORDER BY a.id ASC", Annee.class)
                    .getResultList();
            for (Annee a : list) {
                for (Semestre s : a.getSemestres()) {
                    for (UE u : s.getUes()) {
                        u.getNotes().size();
                    }
                }
            }
            return list;
        });
    }

    public Optional<Annee> findById(int id) {
        return jpaManager.executeInTransactionWithResult(em -> {
            List<Annee> list = em.createQuery("SELECT DISTINCT a FROM Annee a LEFT JOIN FETCH a.semestres WHERE a.id = :id", Annee.class)
                    .setParameter("id", id)
                    .getResultList();
            if (list.isEmpty()) {
                return Optional.empty();
            }
            Annee a = list.get(0);
            for (Semestre s : a.getSemestres()) {
                for (UE u : s.getUes()) {
                    u.getNotes().size();
                }
            }
            return Optional.of(a);
        });
    }

    public Annee save(Annee annee) {
        return jpaManager.executeInTransactionWithResult(em -> {
            if (annee.getId() == 0) {
                em.persist(annee);
                return annee;
            } else {
                return em.merge(annee);
            }
        });
    }

    public void delete(int id) {
        jpaManager.executeInTransaction(em -> {
            Annee a = em.find(Annee.class, id);
            if (a != null) {
                em.remove(a);
            }
        });
    }
}
