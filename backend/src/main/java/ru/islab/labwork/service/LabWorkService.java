package ru.islab.labwork.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import ru.islab.labwork.dao.CatalogDao;
import ru.islab.labwork.dao.LabWorkDao;
import ru.islab.labwork.dto.CoordinatesInput;
import ru.islab.labwork.dto.DeleteInfo;
import ru.islab.labwork.dto.DeleteResult;
import ru.islab.labwork.dto.DisciplineInput;
import ru.islab.labwork.dto.LabWorkBriefDto;
import ru.islab.labwork.dto.LabWorkDto;
import ru.islab.labwork.dto.LabWorkInput;
import ru.islab.labwork.dto.LocationInput;
import ru.islab.labwork.dto.PageDto;
import ru.islab.labwork.dto.PersonInput;
import ru.islab.labwork.entity.Color;
import ru.islab.labwork.entity.Coordinates;
import ru.islab.labwork.entity.Country;
import ru.islab.labwork.entity.Difficulty;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Location;
import ru.islab.labwork.entity.Person;
import ru.islab.labwork.error.ApiException;

@ApplicationScoped
@Transactional
public class LabWorkService {

    private static final Set<String> SORTS = Set.of("name", "description", "difficulty", "disciplineName", "authorName");
    private static final Set<String> COLORS = Set.of("RED", "BLACK", "YELLOW", "BROWN");
    private static final Set<String> COUNTRIES = Set.of("RUSSIA", "GERMANY", "SPAIN", "INDIA");
    private static final Set<String> DIFFICULTIES = Set.of("NORMAL", "HARD", "VERY_HARD", "IMPOSSIBLE", "TERRIBLE");

    @Inject
    LabWorkDao labWorkDao;

    @Inject
    CatalogDao catalogDao;

    @Inject
    Event<ChangeEvent> changes;

    public PageDto<LabWorkDto> list(Integer page,
                                    Integer size,
                                    String name,
                                    String description,
                                    String difficulty,
                                    String disciplineName,
                                    String authorName,
                                    String sort,
                                    String order) {
        int pageIndex = page == null || page < 0 ? 0 : page;
        int pageSize = size == null || size < 1 ? 5 : Math.min(size, 50);
        String sortField = blankToNull(sort);
        if (sortField != null && !SORTS.contains(sortField)) {
            throw ApiException.of(400, "Сортировка доступна только по строковым колонкам");
        }
        boolean descending = false;
        if (order != null && !order.isBlank()) {
            if (!order.equals("asc") && !order.equals("desc")) {
                throw ApiException.of(400, "Порядок сортировки: asc или desc");
            }
            descending = order.equals("desc");
        }
        Difficulty difficultyValue = null;
        String difficultyFilter = blankToNull(difficulty);
        if (difficultyFilter != null) {
            if (!DIFFICULTIES.contains(difficultyFilter)) {
                throw ApiException.of(400, "Недопустимое значение difficulty");
            }
            difficultyValue = Difficulty.valueOf(difficultyFilter);
        }

        LabWorkDao.QueryPage result = labWorkDao.page(
                trimToNull(name),
                blankToNull(description),
                difficultyValue,
                trimToNull(disciplineName),
                trimToNull(authorName),
                sortField,
                descending,
                pageIndex,
                pageSize);

        PageDto<LabWorkDto> dto = new PageDto<>();
        dto.items = result.items().stream().map(item -> LabWorkMapper.toDto(item, null)).toList();
        dto.total = result.total();
        dto.page = pageIndex;
        dto.size = pageSize;
        return dto;
    }

    public LabWorkDto get(int id) {
        LabWork labWork = require(id);
        return LabWorkMapper.toDto(labWork, labWorkDao.programsContaining(id));
    }

    public List<LabWorkBriefDto> briefs() {
        return labWorkDao.allOrdered().stream().map(LabWorkMapper::toBrief).toList();
    }

    public LabWorkDto create(LabWorkInput input) {
        validate(input);
        LabWork labWork = new LabWork();
        apply(labWork, input, null);
        if (labWork.getCreationDate() == null) {
            labWork.setCreationDate(new Date());
        }
        labWorkDao.persist(labWork);
        labWorkDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.CREATE, labWork.getId()));
        return LabWorkMapper.toDto(labWork, List.of());
    }

    public LabWorkDto update(int id, LabWorkInput input) {
        LabWork labWork = require(id);
        validate(input);
        apply(labWork, input, labWork);
        labWorkDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.UPDATE, id));
        return LabWorkMapper.toDto(labWork, labWorkDao.programsContaining(id));
    }

    public DeleteInfo deleteInfo(int id) {
        require(id);
        List<Discipline> owners = labWorkDao.programsContaining(id);
        DeleteInfo info = new DeleteInfo();
        info.referenceCount = owners.size();
        info.requiresReplacement = !owners.isEmpty();
        info.programs = owners.stream().map(LabWorkMapper::toIdName).toList();
        info.message = info.requiresReplacement
                ? "Работа входит в программу дисциплин (" + owners.size()
                + "). Выберите другую лабораторную работу: она займёт место удаляемой, после этого запись будет удалена."
                : "Работа ни в одну программу не входит. Её можно удалить.";
        return info;
    }

    public DeleteResult delete(int id, Integer replacementId) {
        LabWork labWork = require(id);
        List<Discipline> owners = labWorkDao.programsContaining(id);
        Long appliedReplacement = null;
        if (!owners.isEmpty()) {
            if (replacementId == null) {
                throw ApiException.replacement(
                        "Лабораторная работа входит в программу дисциплин. Выберите другую работу, которая займёт её место.");
            }
            if (replacementId.equals(id)) {
                throw ApiException.of(400, "Нельзя заменить объект самим собой");
            }
            LabWork replacement = require(replacementId);
            for (Discipline discipline : owners) {
                discipline.getProgram().removeIf(item -> id == item.getId());
                boolean already = discipline.getProgram().stream().anyMatch(item -> replacementId.equals(item.getId()));
                if (!already) {
                    discipline.getProgram().add(replacement);
                }
            }
            labWorkDao.flush();
            appliedReplacement = replacement.getId().longValue();
        }
        labWorkDao.remove(labWork);
        labWorkDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.DELETE, id));
        DeleteResult result = new DeleteResult();
        result.deletedId = id;
        result.replacementId = appliedReplacement;
        return result;
    }

    private void apply(LabWork labWork, LabWorkInput input, LabWork current) {
        labWork.setName(input.name.trim());
        labWork.setDescription(blankToNull(input.description));
        String difficulty = blankToNull(input.difficulty);
        labWork.setDifficulty(difficulty == null ? null : Difficulty.valueOf(difficulty));
        labWork.setMinimalPoint(input.minimalPoint);
        labWork.setTunedInWorks(input.tunedInWorks);
        labWork.setCoordinates(resolveCoordinates(input.coordinates, current));
        labWork.setDiscipline(resolveDiscipline(input.discipline, current));
        labWork.setAuthor(resolvePerson(input.author, current));
    }

    private void validate(LabWorkInput input) {
        Map<String, String> errors = new LinkedHashMap<>();
        if (input == null) {
            throw ApiException.of(400, "Пустое тело запроса");
        }
        String name = input.name == null ? "" : input.name.trim();
        if (name.isEmpty()) {
            errors.put("name", "Название не может быть пустым");
        } else if (name.length() > 5000) {
            errors.put("name", "Слишком длинное название");
        }
        if (input.description != null && input.description.length() > 2960) {
            errors.put("description", "Длина описания не больше 2960");
        }
        String difficulty = blankToNull(input.difficulty);
        if (difficulty != null && !DIFFICULTIES.contains(difficulty)) {
            errors.put("difficulty", "Недопустимое значение сложности");
        }
        if (input.minimalPoint == null) {
            errors.put("minimalPoint", "Укажите minimalPoint");
        } else if (input.minimalPoint <= 0) {
            errors.put("minimalPoint", "minimalPoint должен быть больше 0");
        }
        validateCoordinates(input.coordinates, errors);
        validateDiscipline(input.discipline, errors);
        validatePerson(input.author, errors);
        if (!errors.isEmpty()) {
            throw ApiException.fields("Проверьте поля формы", errors);
        }
    }

    private void validateCoordinates(CoordinatesInput input, Map<String, String> errors) {
        if (input == null) {
            errors.put("coordinates", "Укажите координаты");
            return;
        }
        boolean values = input.x != null || input.y != null;
        if (input.id != null && !values) {
            if (catalogDao.findCoordinates(input.id) == null) {
                errors.put("coordinates.id", "Координаты не найдены");
            }
            return;
        }
        if (input.x == null) {
            errors.put("coordinates.x", "Укажите x");
        } else if (input.x > 139) {
            errors.put("coordinates.x", "Максимальное значение x — 139");
        }
        if (input.y == null) {
            errors.put("coordinates.y", "Укажите y");
        } else if (!Double.isFinite(input.y)) {
            errors.put("coordinates.y", "y должно быть конечным числом");
        }
        if (input.id != null && catalogDao.findCoordinates(input.id) == null) {
            errors.put("coordinates.id", "Координаты не найдены");
        }
    }

    private void validateDiscipline(DisciplineInput input, Map<String, String> errors) {
        if (input == null) {
            errors.put("discipline", "Укажите дисциплину");
            return;
        }
        boolean values = input.name != null || input.lectureHours != null || input.selfStudyHours != null || input.labsCount != null;
        if (input.id != null && !values) {
            if (catalogDao.findDiscipline(input.id) == null) {
                errors.put("discipline.id", "Дисциплина не найдена");
            }
            return;
        }
        String name = input.name == null ? "" : input.name.trim();
        if (name.isEmpty()) {
            errors.put("discipline.name", "Название дисциплины не может быть пустым");
        }
        if (input.lectureHours == null) {
            errors.put("discipline.lectureHours", "Укажите число лекционных часов");
        }
        if (input.selfStudyHours == null) {
            errors.put("discipline.selfStudyHours", "Укажите часы самостоятельной работы");
        }
        if (input.labsCount == null) {
            errors.put("discipline.labsCount", "Укажите число лабораторных");
        }
        if (input.id != null && catalogDao.findDiscipline(input.id) == null) {
            errors.put("discipline.id", "Дисциплина не найдена");
        }
    }

    private void validatePerson(PersonInput input, Map<String, String> errors) {
        if (input == null) {
            return;
        }
        boolean snapshot = input.name != null || input.hairColor != null || input.eyeColor != null
                || input.weight != null || input.nationality != null || input.location != null;
        if (input.id != null && !snapshot) {
            if (catalogDao.findPerson(input.id) == null) {
                errors.put("author.id", "Автор не найден");
            }
            return;
        }
        String name = input.name == null ? "" : input.name.trim();
        if (name.isEmpty()) {
            errors.put("author.name", "Имя автора не может быть пустым");
        }
        String hair = blankToNull(input.hairColor);
        if (hair == null) {
            errors.put("author.hairColor", "Цвет волос обязателен");
        } else if (!COLORS.contains(hair)) {
            errors.put("author.hairColor", "Недопустимый цвет волос");
        }
        String eye = blankToNull(input.eyeColor);
        if (eye != null && !COLORS.contains(eye)) {
            errors.put("author.eyeColor", "Недопустимый цвет глаз");
        }
        if (input.weight != null && input.weight <= 0) {
            errors.put("author.weight", "Вес должен быть больше 0");
        }
        String nationality = blankToNull(input.nationality);
        if (nationality != null && !COUNTRIES.contains(nationality)) {
            errors.put("author.nationality", "Недопустимая страна");
        }
        validateLocation(input.location, errors);
        if (input.id != null && catalogDao.findPerson(input.id) == null) {
            errors.put("author.id", "Автор не найден");
        }
    }

    private void validateLocation(LocationInput input, Map<String, String> errors) {
        if (input == null) {
            return;
        }
        boolean values = input.x != null || input.y != null || input.z != null;
        if (input.id != null && !values) {
            if (catalogDao.findLocation(input.id) == null) {
                errors.put("author.location.id", "Локация не найдена");
            }
            return;
        }
        if (input.x == null) {
            errors.put("author.location.x", "Укажите x локации");
        }
        if (input.y == null) {
            errors.put("author.location.y", "Укажите y локации");
        }
        if (input.z == null) {
            errors.put("author.location.z", "Укажите z локации");
        } else if (!Double.isFinite(input.z)) {
            errors.put("author.location.z", "z должно быть конечным числом");
        }
        if (input.id != null && catalogDao.findLocation(input.id) == null) {
            errors.put("author.location.id", "Локация не найдена");
        }
    }

    private Coordinates resolveCoordinates(CoordinatesInput input, LabWork current) {
        if (input.id != null && input.x == null && input.y == null) {
            return requireCoordinates(input.id);
        }
        if (input.id != null) {
            Coordinates existing = requireCoordinates(input.id);
            if (!coordinatesDiffer(existing, input)) {
                return existing;
            }
            if (exclusiveCoordinates(existing, current)) {
                existing.setX(input.x);
                existing.setY(input.y.floatValue());
                return existing;
            }
        }
        Coordinates created = new Coordinates();
        created.setX(input.x);
        created.setY(input.y.floatValue());
        catalogDao.persist(created);
        return created;
    }

    private Discipline resolveDiscipline(DisciplineInput input, LabWork current) {
        if (input.id != null && input.name == null && input.lectureHours == null
                && input.selfStudyHours == null && input.labsCount == null) {
            return requireDiscipline(input.id);
        }
        if (input.id != null) {
            Discipline existing = requireDiscipline(input.id);
            if (!disciplineDiffers(existing, input)) {
                return existing;
            }
            if (exclusiveDiscipline(existing, current)) {
                applyDiscipline(existing, input);
                return existing;
            }
        }
        Discipline created = new Discipline();
        applyDiscipline(created, input);
        catalogDao.persist(created);
        return created;
    }

    private Person resolvePerson(PersonInput input, LabWork current) {
        if (input == null) {
            return null;
        }
        boolean snapshot = input.name != null || input.hairColor != null || input.eyeColor != null
                || input.weight != null || input.nationality != null || input.location != null;
        if (input.id != null && !snapshot) {
            return requirePerson(input.id);
        }
        if (input.id != null) {
            Person existing = requirePerson(input.id);
            if (!personDiffers(existing, input)) {
                return existing;
            }
            if (exclusivePerson(existing, current)) {
                applyPerson(existing, input, false);
                return existing;
            }
        }
        Person created = new Person();
        applyPerson(created, input, true);
        catalogDao.persist(created);
        return created;
    }

    private Location resolveLocation(LocationInput input, Person owner) {
        if (input == null) {
            return null;
        }
        if (input.id != null && input.x == null && input.y == null && input.z == null) {
            return requireLocation(input.id);
        }
        if (input.id != null) {
            Location existing = requireLocation(input.id);
            if (!locationDiffers(existing, input)) {
                return existing;
            }
            if (exclusiveLocation(existing, owner)) {
                applyLocation(existing, input);
                return existing;
            }
        }
        Location created = new Location();
        applyLocation(created, input);
        catalogDao.persist(created);
        return created;
    }

    private void applyDiscipline(Discipline discipline, DisciplineInput input) {
        discipline.setName(input.name.trim());
        discipline.setLectureHours(input.lectureHours);
        discipline.setSelfStudyHours(input.selfStudyHours);
        discipline.setLabsCount(input.labsCount);
    }

    private void applyPerson(Person person, PersonInput input, boolean copy) {
        person.setName(input.name.trim());
        person.setEyeColor(enumOrNull(blankToNull(input.eyeColor), Color.class));
        person.setHairColor(Color.valueOf(blankToNull(input.hairColor)));
        person.setWeight(input.weight);
        person.setNationality(enumOrNull(blankToNull(input.nationality), Country.class));
        person.setLocation(resolveLocation(input.location, copy ? null : person));
    }

    private void applyLocation(Location location, LocationInput input) {
        location.setX(input.x);
        location.setY(input.y);
        location.setZ(input.z.floatValue());
    }

    private boolean coordinatesDiffer(Coordinates coordinates, CoordinatesInput input) {
        return coordinates.getX() != input.x || Float.compare(coordinates.getY(), input.y.floatValue()) != 0;
    }

    private boolean disciplineDiffers(Discipline discipline, DisciplineInput input) {
        return !discipline.getName().equals(input.name.trim())
                || !Objects.equals(discipline.getLectureHours(), input.lectureHours)
                || discipline.getSelfStudyHours() != input.selfStudyHours
                || discipline.getLabsCount() != input.labsCount;
    }

    private boolean personDiffers(Person person, PersonInput input) {
        String eye = person.getEyeColor() == null ? null : person.getEyeColor().name();
        String nationality = person.getNationality() == null ? null : person.getNationality().name();
        if (!person.getName().equals(input.name.trim())) {
            return true;
        }
        if (!Objects.equals(eye, blankToNull(input.eyeColor))) {
            return true;
        }
        if (!person.getHairColor().name().equals(blankToNull(input.hairColor))) {
            return true;
        }
        if (!Objects.equals(person.getWeight(), input.weight)) {
            return true;
        }
        if (!Objects.equals(nationality, blankToNull(input.nationality))) {
            return true;
        }
        return locationLinkDiffers(person.getLocation(), input.location);
    }

    private boolean locationLinkDiffers(Location current, LocationInput input) {
        if (input == null) {
            return current != null;
        }
        if (current == null) {
            return true;
        }
        if (input.x == null && input.y == null && input.z == null) {
            return !current.getId().equals(input.id);
        }
        return locationDiffers(current, input);
    }

    private boolean locationDiffers(Location location, LocationInput input) {
        return !Objects.equals(location.getX(), input.x)
                || !Objects.equals(location.getY(), input.y)
                || Float.compare(location.getZ(), input.z.floatValue()) != 0;
    }

    private boolean exclusiveCoordinates(Coordinates coordinates, LabWork current) {
        long refs = catalogDao.countLabWorksByCoordinates(coordinates.getId());
        return refs <= 0 || (refs == 1 && current != null && current.getCoordinates() != null
                && coordinates.getId().equals(current.getCoordinates().getId()));
    }

    private boolean exclusiveDiscipline(Discipline discipline, LabWork current) {
        long refs = catalogDao.countLabWorksByDiscipline(discipline.getId());
        return refs <= 0 || (refs == 1 && current != null && current.getDiscipline() != null
                && discipline.getId().equals(current.getDiscipline().getId()));
    }

    private boolean exclusivePerson(Person person, LabWork current) {
        long refs = catalogDao.countLabWorksByAuthor(person.getId());
        return refs <= 0 || (refs == 1 && current != null && current.getAuthor() != null
                && person.getId().equals(current.getAuthor().getId()));
    }

    private boolean exclusiveLocation(Location location, Person owner) {
        long refs = catalogDao.countPersonsByLocation(location.getId());
        return refs <= 0 || (refs == 1 && owner != null && owner.getLocation() != null
                && location.getId().equals(owner.getLocation().getId()));
    }

    private LabWork require(int id) {
        LabWork labWork = labWorkDao.find(id);
        if (labWork == null) {
            throw ApiException.of(404, "Лабораторная работа не найдена");
        }
        return labWork;
    }

    private Coordinates requireCoordinates(Long id) {
        Coordinates coordinates = catalogDao.findCoordinates(id);
        if (coordinates == null) {
            throw ApiException.of(400, "Координаты не найдены");
        }
        return coordinates;
    }

    private Discipline requireDiscipline(Long id) {
        Discipline discipline = catalogDao.findDiscipline(id);
        if (discipline == null) {
            throw ApiException.of(400, "Дисциплина не найдена");
        }
        return discipline;
    }

    private Person requirePerson(Long id) {
        Person person = catalogDao.findPerson(id);
        if (person == null) {
            throw ApiException.of(400, "Автор не найден");
        }
        return person;
    }

    private Location requireLocation(Long id) {
        Location location = catalogDao.findLocation(id);
        if (location == null) {
            throw ApiException.of(400, "Локация не найдена");
        }
        return location;
    }

    private static <E extends Enum<E>> E enumOrNull(String value, Class<E> type) {
        return value == null ? null : Enum.valueOf(type, value);
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
