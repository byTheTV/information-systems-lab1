package ru.islab.labwork.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.List;
import ru.islab.labwork.entity.Difficulty;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Person;

@ApplicationScoped
public class LabWorkDao {

    @PersistenceContext(unitName = "labworkPU")
    EntityManager em;

    public LabWork find(Integer id) {
        return em.find(LabWork.class, id);
    }

    public void persist(Object entity) {
        em.persist(entity);
    }

    public void remove(Object entity) {
        em.remove(entity);
    }

    public void flush() {
        em.flush();
    }

    public void refresh(Object entity) {
        em.refresh(entity);
    }

    public List<LabWork> allOrdered() {
        return em.createQuery("SELECT l FROM LabWork l ORDER BY l.id", LabWork.class).getResultList();
    }

    public List<Discipline> programsContaining(Integer labWorkId) {
        return em.createQuery(
                        "SELECT d FROM Discipline d JOIN d.program lw WHERE lw.id = :id ORDER BY d.id",
                        Discipline.class)
                .setParameter("id", labWorkId)
                .getResultList();
    }

    public QueryPage page(String name,
                          String description,
                          Difficulty difficulty,
                          String disciplineName,
                          String authorName,
                          String sort,
                          boolean descending,
                          int page,
                          int size) {
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<LabWork> countRoot = countQuery.from(LabWork.class);
        Join<LabWork, Discipline> countDiscipline = countRoot.join("discipline", JoinType.INNER);
        Join<LabWork, Person> countAuthor = countRoot.join("author", JoinType.LEFT);
        countQuery.select(cb.count(countRoot));
        countQuery.where(filters(cb, countRoot, countDiscipline, countAuthor, name, description, difficulty, disciplineName, authorName));
        long total = em.createQuery(countQuery).getSingleResult();

        CriteriaQuery<LabWork> query = cb.createQuery(LabWork.class);
        Root<LabWork> root = query.from(LabWork.class);
        Join<LabWork, Discipline> discipline = root.join("discipline", JoinType.INNER);
        Join<LabWork, Person> author = root.join("author", JoinType.LEFT);
        query.select(root);
        query.where(filters(cb, root, discipline, author, name, description, difficulty, disciplineName, authorName));
        Expression<?> sortExpr = sortExpression(root, discipline, author, sort);
        if (descending) {
            query.orderBy(cb.desc(sortExpr), cb.asc(root.get("id")));
        } else {
            query.orderBy(cb.asc(sortExpr), cb.asc(root.get("id")));
        }

        TypedQuery<LabWork> typed = em.createQuery(query);
        typed.setFirstResult(page * size);
        typed.setMaxResults(size);
        return new QueryPage(typed.getResultList(), total);
    }

    private Predicate[] filters(CriteriaBuilder cb,
                                Root<LabWork> root,
                                Join<LabWork, Discipline> discipline,
                                Join<LabWork, Person> author,
                                String name,
                                String description,
                                Difficulty difficulty,
                                String disciplineName,
                                String authorName) {
        List<Predicate> predicates = new ArrayList<>();
        if (name != null) {
            predicates.add(cb.equal(root.get("name"), name));
        }
        if (description != null) {
            predicates.add(cb.equal(root.get("description"), description));
        }
        if (difficulty != null) {
            predicates.add(cb.equal(root.get("difficulty"), difficulty));
        }
        if (disciplineName != null) {
            predicates.add(cb.equal(discipline.get("name"), disciplineName));
        }
        if (authorName != null) {
            predicates.add(cb.equal(author.get("name"), authorName));
        }
        return predicates.toArray(Predicate[]::new);
    }

    private Expression<?> sortExpression(Root<LabWork> root,
                                         Join<LabWork, Discipline> discipline,
                                         Join<LabWork, Person> author,
                                         String sort) {
        if (sort == null) {
            return root.get("id");
        }
        return switch (sort) {
            case "name" -> root.get("name");
            case "description" -> root.get("description");
            case "difficulty" -> root.get("difficulty");
            case "disciplineName" -> discipline.get("name");
            case "authorName" -> author.get("name");
            default -> root.get("id");
        };
    }

    public record QueryPage(List<LabWork> items, long total) {
    }
}
