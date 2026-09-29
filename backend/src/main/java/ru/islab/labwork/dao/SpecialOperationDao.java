package ru.islab.labwork.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;

@ApplicationScoped
public class SpecialOperationDao {

    @PersistenceContext(unitName = "labworkPU")
    EntityManager em;

    public Object averageMinimalPoint() {
        return em.createNativeQuery("SELECT avg_minimal_point()").getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Number> labWorkIdsByDescription(String substring) {
        return em.createNativeQuery("SELECT id FROM lab_works_by_description(?1)")
                .setParameter(1, substring)
                .getResultList();
    }

    @SuppressWarnings("unchecked")
    public List<Number> uniqueMinimalPoints() {
        return em.createNativeQuery("SELECT point_value FROM unique_minimal_points()").getResultList();
    }

    public Object decreaseDifficulty(int labWorkId, int steps) {
        return em.createNativeQuery("SELECT decrease_labwork_difficulty(?1, ?2)")
                .setParameter(1, labWorkId)
                .setParameter(2, steps)
                .getSingleResult();
    }

    @SuppressWarnings("unchecked")
    public List<Object[]> addHardest(long disciplineId) {
        return em.createNativeQuery("SELECT work_id, inserted_now FROM add_hardest_labworks(?1)")
                .setParameter(1, disciplineId)
                .getResultList();
    }
}
