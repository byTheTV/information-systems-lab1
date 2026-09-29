package ru.islab.labwork.service;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Event;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.List;
import ru.islab.labwork.dao.LabWorkDao;
import ru.islab.labwork.dao.SpecialOperationDao;
import ru.islab.labwork.dto.AddHardestRequest;
import ru.islab.labwork.dto.AddHardestResponse;
import ru.islab.labwork.dto.AverageResponse;
import ru.islab.labwork.dto.DecreaseRequest;
import ru.islab.labwork.dto.LabWorkDto;
import ru.islab.labwork.dto.UniquePointsResponse;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.error.ApiException;

@ApplicationScoped
@Transactional
public class SpecialOperationService {

    @Inject
    SpecialOperationDao specialOperationDao;

    @Inject
    LabWorkDao labWorkDao;

    @Inject
    Event<ChangeEvent> changes;

    public AverageResponse average() {
        Object value = specialOperationDao.averageMinimalPoint();
        AverageResponse response = new AverageResponse();
        if (value instanceof Number number) {
            response.average = number.doubleValue();
        }
        return response;
    }

    public List<LabWorkDto> byDescription(String substring) {
        if (substring == null || substring.isBlank()) {
            throw ApiException.of(400, "Укажите подстроку");
        }
        List<LabWorkDto> result = new ArrayList<>();
        for (Number id : specialOperationDao.labWorkIdsByDescription(substring)) {
            LabWork labWork = labWorkDao.find(id.intValue());
            if (labWork != null) {
                result.add(LabWorkMapper.toDto(labWork, labWorkDao.programsContaining(labWork.getId())));
            }
        }
        return result;
    }

    public UniquePointsResponse uniquePoints() {
        UniquePointsResponse response = new UniquePointsResponse();
        response.values = new ArrayList<>();
        for (Number value : specialOperationDao.uniqueMinimalPoints()) {
            response.values.add(value.longValue());
        }
        return response;
    }

    public LabWorkDto decrease(DecreaseRequest request) {
        if (request == null || request.labWorkId == null) {
            throw ApiException.of(400, "Не указана лабораторная работа");
        }
        if (request.steps == null || request.steps <= 0) {
            throw ApiException.of(400, "Число шагов должно быть больше 0");
        }
        specialOperationDao.decreaseDifficulty(request.labWorkId, request.steps);
        LabWork labWork = labWorkDao.find(request.labWorkId);
        if (labWork == null) {
            throw ApiException.of(404, "Лабораторная работа не найдена");
        }
        labWorkDao.refresh(labWork);
        changes.fire(new ChangeEvent(ChangeEvent.UPDATE, labWork.getId()));
        return LabWorkMapper.toDto(labWork, labWorkDao.programsContaining(labWork.getId()));
    }

    public AddHardestResponse addHardest(AddHardestRequest request) {
        if (request == null || request.disciplineId == null) {
            throw ApiException.of(400, "Не указана дисциплина");
        }
        List<?> rows = specialOperationDao.addHardest(request.disciplineId);
        AddHardestResponse response = new AddHardestResponse();
        response.labWorkIds = new ArrayList<>();
        for (Object row : rows) {
            Object[] columns = row instanceof Object[] array ? array : new Object[] {row};
            response.labWorkIds.add(((Number) columns[0]).longValue());
            if (columns.length > 1 && Boolean.TRUE.equals(asBoolean(columns[1]))) {
                response.inserted++;
            }
        }
        changes.fire(new ChangeEvent(ChangeEvent.RELOAD, null));
        return response;
    }

    private static Boolean asBoolean(Object value) {
        if (value instanceof Boolean bool) {
            return bool;
        }
        if (value instanceof Number number) {
            return number.intValue() != 0;
        }
        return Boolean.valueOf(String.valueOf(value));
    }
}
