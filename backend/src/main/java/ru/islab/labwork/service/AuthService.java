package ru.islab.labwork.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.UUID;
import java.util.Date;
import ru.islab.labwork.dao.UserDao;
import ru.islab.labwork.dto.AuthResponse;
import ru.islab.labwork.dto.Credentials;
import ru.islab.labwork.entity.AppUser;
import ru.islab.labwork.entity.UserSession;
import ru.islab.labwork.error.ApiException;
import ru.islab.labwork.security.PasswordHasher;
import ru.islab.labwork.security.RequestUser;
import ru.islab.labwork.security.Roles;

@ApplicationScoped
@Transactional
public class AuthService {

    @Inject
    UserDao userDao;

    public AuthResponse register(Credentials credentials) {
        String username = username(credentials);
        String password = password(credentials);
        if (userDao.findByUsername(username) != null) {
            throw ApiException.of(409, "Пользователь с таким именем уже есть");
        }
        return startSession(createUser(username, password, Roles.USER));
    }

    public AuthResponse login(Credentials credentials) {
        String username = credentials == null || credentials.username == null ? "" : credentials.username.trim();
        String password = credentials == null || credentials.password == null ? "" : credentials.password;
        AppUser user = userDao.findByUsername(username);
        if (user == null || !PasswordHasher.verify(password, user.getPasswordHash())) {
            throw ApiException.of(401, "Неверное имя или пароль");
        }
        return startSession(user);
    }

    public void logout(String token) {
        userDao.remove(userDao.findSession(token));
    }

    public RequestUser authenticate(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        UserSession session = userDao.findSession(token);
        if (session == null) {
            return null;
        }
        AppUser user = session.getUser();
        return new RequestUser(user.getId(), user.getUsername(), user.getRole());
    }

    public void ping() {
        userDao.countUsers();
    }

    private AuthResponse startSession(AppUser user) {
        UserSession session = new UserSession();
        session.setToken(UUID.randomUUID().toString());
        session.setUser(user);
        session.setCreatedAt(new Date());
        userDao.persist(session);
        AuthResponse response = new AuthResponse();
        response.token = session.getToken();
        response.username = user.getUsername();
        response.role = user.getRole();
        return response;
    }

    private AppUser createUser(String username, String password, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setRole(role);
        userDao.persist(user);
        return user;
    }

    private static String username(Credentials credentials) {
        String username = credentials == null || credentials.username == null ? "" : credentials.username.trim();
        if (!username.matches("[A-Za-z0-9_]{3,32}")) {
            throw ApiException.of(400, "Имя пользователя: 3–32 символа, латиница, цифры и _");
        }
        return username;
    }

    private static String password(Credentials credentials) {
        String password = credentials == null || credentials.password == null ? "" : credentials.password;
        if (password.length() < 4 || password.length() > 128) {
            throw ApiException.of(400, "Пароль: от 4 до 128 символов");
        }
        return password;
    }
}
