package ru.islab.labwork.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import ru.islab.labwork.entity.Coordinates;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Location;
import ru.islab.labwork.entity.Person;

@ApplicationScoped
public class CatalogDao {

    @PersistenceContext(unitName = "labworkPU")
    EntityManager em;

    public void persist(Object entity) {
        em.persist(entity);
    }

    public void remove(Object entity) {
        em.remove(entity);
    }

    public void flush() {
        em.flush();
    }

    public Coordinates findCoordinates(Long id) {
        return id == null ? null : em.find(Coordinates.class, id);
    }

    public Discipline findDiscipline(Long id) {
        return id == null ? null : em.find(Discipline.class, id);
    }

    public Person findPerson(Long id) {
        return id == null ? null : em.find(Person.class, id);
    }

    public Location findLocation(Long id) {
        return id == null ? null : em.find(Location.class, id);
    }

    public List<Coordinates> allCoordinates() {
        return em.createQuery("SELECT c FROM Coordinates c ORDER BY c.id", Coordinates.class).getResultList();
    }

    public List<Discipline> allDisciplines() {
        return em.createQuery("SELECT d FROM Discipline d ORDER BY d.id", Discipline.class).getResultList();
    }

    public List<Person> allPersons() {
        return em.createQuery("SELECT p FROM Person p ORDER BY p.id", Person.class).getResultList();
    }

    public List<Location> allLocations() {
        return em.createQuery("SELECT l FROM Location l ORDER BY l.id", Location.class).getResultList();
    }

    public long countLabWorksByCoordinates(Long id) {
        return count("SELECT COUNT(l) FROM LabWork l WHERE l.coordinates.id = :id", id);
    }

    public long countLabWorksByDiscipline(Long id) {
        return count("SELECT COUNT(l) FROM LabWork l WHERE l.discipline.id = :id", id);
    }

    public long countLabWorksByAuthor(Long id) {
        return count("SELECT COUNT(l) FROM LabWork l WHERE l.author.id = :id", id);
    }

    public long countPersonsByLocation(Long id) {
        return count("SELECT COUNT(p) FROM Person p WHERE p.location.id = :id", id);
    }

    public List<LabWork> labWorksByCoordinates(Long id) {
        return em.createQuery("SELECT l FROM LabWork l WHERE l.coordinates.id = :id", LabWork.class)
                .setParameter("id", id)
                .getResultList();
    }

    public List<LabWork> labWorksByDiscipline(Long id) {
        return em.createQuery("SELECT l FROM LabWork l WHERE l.discipline.id = :id", LabWork.class)
                .setParameter("id", id)
                .getResultList();
    }

    public List<LabWork> labWorksByAuthor(Long id) {
        return em.createQuery("SELECT l FROM LabWork l WHERE l.author.id = :id", LabWork.class)
                .setParameter("id", id)
                .getResultList();
    }

    public List<Person> personsByLocation(Long id) {
        return em.createQuery("SELECT p FROM Person p WHERE p.location.id = :id", Person.class)
                .setParameter("id", id)
                .getResultList();
    }

    private long count(String jpql, Long id) {
        return em.createQuery(jpql, Long.class).setParameter("id", id).getSingleResult();
    }
}
