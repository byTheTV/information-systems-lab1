package ru.islab.labwork.dao;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import ru.islab.labwork.entity.AppUser;
import ru.islab.labwork.entity.UserSession;

@ApplicationScoped
public class UserDao {

    @PersistenceContext(unitName = "labworkPU")
    EntityManager em;

    public long countUsers() {
        return em.createQuery("SELECT COUNT(u) FROM AppUser u", Long.class).getSingleResult();
    }

    public AppUser findByUsername(String username) {
        List<AppUser> users = em.createQuery("SELECT u FROM AppUser u WHERE u.username = :username", AppUser.class)
                .setParameter("username", username)
                .setMaxResults(1)
                .getResultList();
        return users.isEmpty() ? null : users.get(0);
    }

    public UserSession findSession(String token) {
        return token == null ? null : em.find(UserSession.class, token);
    }

    public void persist(Object entity) {
        em.persist(entity);
    }

    public void remove(Object entity) {
        if (entity != null) {
            em.remove(entity);
        }
    }
}
