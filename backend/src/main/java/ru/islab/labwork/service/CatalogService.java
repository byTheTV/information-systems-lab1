package ru.islab.labwork.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.HashSet;
import java.util.List;
import ru.islab.labwork.dao.CatalogDao;
import ru.islab.labwork.dto.CoordinatesDto;
import ru.islab.labwork.dto.DeleteInfo;
import ru.islab.labwork.dto.DeleteResult;
import ru.islab.labwork.dto.DisciplineDto;
import ru.islab.labwork.dto.LocationDto;
import ru.islab.labwork.dto.PersonDto;
import ru.islab.labwork.entity.Coordinates;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Location;
import ru.islab.labwork.entity.Person;
import ru.islab.labwork.error.ApiException;

@ApplicationScoped
@Transactional
public class CatalogService {

    @Inject
    CatalogDao catalogDao;

    @Inject
    Event<ChangeEvent> changes;

    public List<CoordinatesDto> coordinates() {
        return catalogDao.allCoordinates().stream().map(LabWorkMapper::toCoordinates).toList();
    }

    public List<DisciplineDto> disciplines() {
        return catalogDao.allDisciplines().stream().map(LabWorkMapper::toDiscipline).toList();
    }

    public List<PersonDto> persons() {
        return catalogDao.allPersons().stream().map(LabWorkMapper::toPerson).toList();
    }

    public List<LocationDto> locations() {
        return catalogDao.allLocations().stream().map(LabWorkMapper::toLocation).toList();
    }

    public DeleteInfo coordinatesInfo(long id) {
        requireCoordinates(id);
        long refs = catalogDao.countLabWorksByCoordinates(id);
        return info(refs, "Координаты используют лабораторные работы (" + refs
                + "). Выберите другие координаты: ссылки будут перенесены, затем запись удалится.",
                "На эти координаты никто не ссылается. Их можно удалить.");
    }

    public DeleteResult deleteCoordinates(long id, Long replacementId) {
        Coordinates source = requireCoordinates(id);
        reassign(catalogDao.countLabWorksByCoordinates(id), replacementId, id, "координаты", () -> {
            Coordinates target = requireCoordinates(replacementId);
            for (LabWork labWork : catalogDao.labWorksByCoordinates(id)) {
                labWork.setCoordinates(target);
            }
        });
        catalogDao.remove(source);
        catalogDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.RELOAD, null));
        return result(id, replacementId, catalogDao.countLabWorksByCoordinates(id));
    }

    public DeleteInfo disciplineInfo(long id) {
        Discipline discipline = requireDiscipline(id);
        long refs = catalogDao.countLabWorksByDiscipline(id);
        int program = discipline.getProgram().size();
        DeleteInfo info = new DeleteInfo();
        info.referenceCount = refs + program;
        info.requiresReplacement = refs > 0 || program > 0;
        info.message = info.requiresReplacement
                ? "На дисциплину ссылаются лабораторные работы (" + refs + "), в программе записей: " + program
                + ". Выберите другую дисциплину: и поле discipline, и программа будут перенесены, затем запись удалится."
                : "На дисциплину никто не ссылается. Её можно удалить.";
        return info;
    }

    public DeleteResult deleteDiscipline(long id, Long replacementId) {
        Discipline source = requireDiscipline(id);
        long refs = catalogDao.countLabWorksByDiscipline(id);
        int program = source.getProgram().size();
        boolean used = refs > 0 || program > 0;
        Long applied = null;
        if (used) {
            ensureReplacement(replacementId, id, "дисциплину");
            Discipline target = requireDiscipline(replacementId);
            for (LabWork labWork : catalogDao.labWorksByDiscipline(id)) {
                labWork.setDiscipline(target);
            }
            for (LabWork labWork : new HashSet<>(source.getProgram())) {
                source.getProgram().remove(labWork);
                target.getProgram().add(labWork);
            }
            catalogDao.flush();
            applied = replacementId;
        }
        catalogDao.remove(source);
        catalogDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.RELOAD, null));
        DeleteResult result = new DeleteResult();
        result.deletedId = id;
        result.replacementId = applied;
        return result;
    }

    public DeleteInfo personInfo(long id) {
        requirePerson(id);
        long refs = catalogDao.countLabWorksByAuthor(id);
        return info(refs, "Этот автор указан у лабораторных работ (" + refs
                + "). Выберите другого автора: ссылки будут перенесены, затем запись удалится.",
                "Как автор эта запись не используется. Её можно удалить.");
    }

    public DeleteResult deletePerson(long id, Long replacementId) {
        Person source = requirePerson(id);
        long refs = catalogDao.countLabWorksByAuthor(id);
        Long applied = null;
        if (refs > 0) {
            ensureReplacement(replacementId, id, "автора");
            Person target = requirePerson(replacementId);
            for (LabWork labWork : catalogDao.labWorksByAuthor(id)) {
                labWork.setAuthor(target);
            }
            catalogDao.flush();
            applied = replacementId;
        }
        catalogDao.remove(source);
        catalogDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.RELOAD, null));
        DeleteResult result = new DeleteResult();
        result.deletedId = id;
        result.replacementId = applied;
        return result;
    }

    public DeleteInfo locationInfo(long id) {
        requireLocation(id);
        long refs = catalogDao.countPersonsByLocation(id);
        return info(refs, "Локацию используют авторы (" + refs
                + "). Выберите другую локацию: ссылки будут перенесены, затем запись удалится.",
                "На локацию никто не ссылается. Её можно удалить.");
    }

    public DeleteResult deleteLocation(long id, Long replacementId) {
        Location source = requireLocation(id);
        long refs = catalogDao.countPersonsByLocation(id);
        Long applied = null;
        if (refs > 0) {
            ensureReplacement(replacementId, id, "локацию");
            Location target = requireLocation(replacementId);
            for (Person person : catalogDao.personsByLocation(id)) {
                person.setLocation(target);
            }
            catalogDao.flush();
            applied = replacementId;
        }
        catalogDao.remove(source);
        catalogDao.flush();
        changes.fire(new ChangeEvent(ChangeEvent.RELOAD, null));
        DeleteResult result = new DeleteResult();
        result.deletedId = id;
        result.replacementId = applied;
        return result;
    }

    private void reassign(long refs, Long replacementId, long id, String label, Runnable action) {
        if (refs <= 0) {
            return;
        }
        ensureReplacement(replacementId, id, label);
        action.run();
        catalogDao.flush();
    }

    private DeleteResult result(long id, Long replacementId, long refsAfter) {
        DeleteResult result = new DeleteResult();
        result.deletedId = id;
        result.replacementId = refsAfter > 0 ? null : replacementId;
        return result;
    }

    private void ensureReplacement(Long replacementId, long id, String label) {
        if (replacementId == null) {
            throw ApiException.replacement("Чтобы удалить " + label + ", выберите другой объект того же типа.");
        }
        if (replacementId == id) {
            throw ApiException.of(400, "Нельзя заменить объект самим собой");
        }
    }

    private DeleteInfo info(long refs, String used, String free) {
        DeleteInfo info = new DeleteInfo();
        info.referenceCount = refs;
        info.requiresReplacement = refs > 0;
        info.message = refs > 0 ? used : free;
        return info;
    }

    private Coordinates requireCoordinates(long id) {
        Coordinates coordinates = catalogDao.findCoordinates(id);
        if (coordinates == null) {
            throw ApiException.of(404, "Координаты не найдены");
        }
        return coordinates;
    }

    private Discipline requireDiscipline(long id) {
        Discipline discipline = catalogDao.findDiscipline(id);
        if (discipline == null) {
            throw ApiException.of(404, "Дисциплина не найдена");
        }
        return discipline;
    }

    private Person requirePerson(long id) {
        Person person = catalogDao.findPerson(id);
        if (person == null) {
            throw ApiException.of(404, "Автор не найден");
        }
        return person;
    }

    private Location requireLocation(long id) {
        Location location = catalogDao.findLocation(id);
        if (location == null) {
            throw ApiException.of(404, "Локация не найдена");
        }
        return location;
    }
}
