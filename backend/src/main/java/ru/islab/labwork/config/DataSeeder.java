package ru.islab.labwork.config;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import ru.islab.labwork.dao.CatalogDao;
import ru.islab.labwork.dao.LabWorkDao;
import ru.islab.labwork.dao.UserDao;
import ru.islab.labwork.entity.Color;
import ru.islab.labwork.entity.Coordinates;
import ru.islab.labwork.entity.Country;
import ru.islab.labwork.entity.Difficulty;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Location;
import ru.islab.labwork.entity.Person;
import ru.islab.labwork.security.PasswordHasher;
import ru.islab.labwork.security.Roles;
import ru.islab.labwork.entity.AppUser;

@ApplicationScoped
@Transactional
public class DataSeeder {

    @Inject
    UserDao userDao;

    @Inject
    CatalogDao catalogDao;

    @Inject
    LabWorkDao labWorkDao;

    public void seed() {
        if (userDao.countUsers() > 0) {
            return;
        }
        user("admin", "admin", Roles.ADMIN);
        user("viewer", "viewer", Roles.USER);
        seedLabWorks();
    }

    private void user(String username, String password, String role) {
        AppUser user = new AppUser();
        user.setUsername(username);
        user.setPasswordHash(PasswordHasher.hash(password));
        user.setRole(role);
        userDao.persist(user);
    }

    private void seedLabWorks() {
        Location petersburg = location(59, 30L, 12.5f);
        Location berlin = location(13, 52L, 0f);

        Person ivanov = person("Иванов", Color.RED, Color.BLACK, petersburg, 74L, Country.RUSSIA);
        Person schmidt = person("Schmidt", null, Color.BROWN, null, null, Country.GERMANY);
        Person garcia = person("Garcia", Color.YELLOW, Color.BLACK, berlin, 61L, Country.SPAIN);
        Person[] authors = {ivanov, schmidt, garcia};

        Coordinates[] coordinates = {
                coordinates(10, 3.5f),
                coordinates(139, -2f),
                coordinates(0, 1f),
                coordinates(-20, 8.25f)
        };

        Discipline informatics = discipline("Информатика", 32, 40, 8);
        Discipline databases = discipline("Базы данных", 48, 36, 12);
        Discipline networks = discipline("Сети", 24, 16, 4);
        Discipline[] disciplines = {informatics, databases, networks};

        String[] names = {
                "Сортировка слиянием",
                "Индексация",
                "Нормализация",
                "Транзакции",
                "Маршрутизация",
                "Сокеты",
                "Хеширование",
                "Планировщик",
                "Репликация",
                "Файрвол",
                "Кэш",
                "Представления"
        };
        String[] descriptions = {
                "Внешняя сортировка и алгоритм слияния прогонов",
                "B-дерево и алгоритм вставки",
                null,
                "Уровни изоляции",
                "Протокол OSPF",
                null,
                "Открытая адресация",
                "Очередь с приоритетом",
                "Синхронная репликация журнала",
                "Фильтрация пакетов",
                "Политика вытеснения LRU",
                "Обновляемые представления"
        };
        Difficulty[] difficulties = {
                Difficulty.VERY_HARD,
                Difficulty.HARD,
                Difficulty.NORMAL,
                Difficulty.IMPOSSIBLE,
                Difficulty.HARD,
                null,
                Difficulty.TERRIBLE,
                Difficulty.VERY_HARD,
                Difficulty.HARD,
                Difficulty.NORMAL,
                Difficulty.IMPOSSIBLE,
                Difficulty.NORMAL
        };
        long[] points = {80, 60, 40, 100, 55, 25, 90, 70, 60, 15, 85, 40};
        Integer[] tuned = {3, 2, null, 5, 1, null, 4, 2, null, 1, 6, 0};
        int[] coordinateIndex = {0, 1, 2, 0, 3, 1, 2, 3, 0, 1, 2, 3};
        int[] disciplineIndex = {0, 1, 1, 1, 2, 2, 0, 0, 1, 2, 0, 1};
        Integer[] authorIndex = {0, 0, 1, 2, null, 1, 0, 2, null, 1, 0, 2};

        List<LabWork> works = new ArrayList<>();
        for (int i = 0; i < names.length; i++) {
            LabWork labWork = new LabWork();
            labWork.setName(names[i]);
            labWork.setDescription(descriptions[i]);
            labWork.setDifficulty(difficulties[i]);
            labWork.setMinimalPoint(points[i]);
            labWork.setTunedInWorks(tuned[i]);
            labWork.setCoordinates(coordinates[coordinateIndex[i]]);
            labWork.setDiscipline(disciplines[disciplineIndex[i]]);
            labWork.setAuthor(authorIndex[i] == null ? null : authors[authorIndex[i]]);
            labWork.setCreationDate(Date.from(Instant.parse("2026-09-01T08:00:00Z").plus(Duration.ofHours(i * 6L))));
            labWorkDao.persist(labWork);
            works.add(labWork);
        }
        labWorkDao.flush();
        informatics.getProgram().add(works.get(0));
        informatics.getProgram().add(works.get(6));
        databases.getProgram().add(works.get(3));
        labWorkDao.flush();
    }

    private Location location(int x, long y, float z) {
        Location location = new Location();
        location.setX(x);
        location.setY(y);
        location.setZ(z);
        catalogDao.persist(location);
        return location;
    }

    private Person person(String name, Color eye, Color hair, Location location, Long weight, Country nationality) {
        Person person = new Person();
        person.setName(name);
        person.setEyeColor(eye);
        person.setHairColor(hair);
        person.setLocation(location);
        person.setWeight(weight);
        person.setNationality(nationality);
        catalogDao.persist(person);
        return person;
    }

    private Coordinates coordinates(long x, float y) {
        Coordinates coordinates = new Coordinates();
        coordinates.setX(x);
        coordinates.setY(y);
        catalogDao.persist(coordinates);
        return coordinates;
    }

    private Discipline discipline(String name, int lectures, long selfStudy, int labs) {
        Discipline discipline = new Discipline();
        discipline.setName(name);
        discipline.setLectureHours(lectures);
        discipline.setSelfStudyHours(selfStudy);
        discipline.setLabsCount(labs);
        catalogDao.persist(discipline);
        return discipline;
    }
}
