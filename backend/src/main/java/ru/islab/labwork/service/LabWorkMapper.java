package ru.islab.labwork.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import ru.islab.labwork.dto.CoordinatesDto;
import ru.islab.labwork.dto.DisciplineDto;
import ru.islab.labwork.dto.IdName;
import ru.islab.labwork.dto.LabWorkBriefDto;
import ru.islab.labwork.dto.LabWorkDto;
import ru.islab.labwork.dto.LocationDto;
import ru.islab.labwork.dto.PersonDto;
import ru.islab.labwork.entity.Coordinates;
import ru.islab.labwork.entity.Discipline;
import ru.islab.labwork.entity.LabWork;
import ru.islab.labwork.entity.Location;
import ru.islab.labwork.entity.Person;

public final class LabWorkMapper {

    private static final DateTimeFormatter INSTANT = DateTimeFormatter.ISO_INSTANT;

    private LabWorkMapper() {
    }

    public static LabWorkDto toDto(LabWork labWork, List<Discipline> programs) {
        LabWorkDto dto = new LabWorkDto();
        dto.id = labWork.getId();
        dto.name = labWork.getName();
        dto.coordinates = toCoordinates(labWork.getCoordinates());
        dto.creationDate = INSTANT.format(labWork.getCreationDate().toInstant());
        dto.description = labWork.getDescription();
        dto.difficulty = labWork.getDifficulty() == null ? null : labWork.getDifficulty().name();
        dto.discipline = toDiscipline(labWork.getDiscipline());
        dto.minimalPoint = labWork.getMinimalPoint();
        dto.tunedInWorks = labWork.getTunedInWorks();
        dto.author = labWork.getAuthor() == null ? null : toPerson(labWork.getAuthor());
        if (programs != null) {
            dto.programs = programs.stream().map(LabWorkMapper::toIdName).toList();
        }
        return dto;
    }

    public static LabWorkBriefDto toBrief(LabWork labWork) {
        LabWorkBriefDto dto = new LabWorkBriefDto();
        dto.id = labWork.getId();
        dto.name = labWork.getName();
        dto.difficulty = labWork.getDifficulty() == null ? null : labWork.getDifficulty().name();
        return dto;
    }

    public static CoordinatesDto toCoordinates(Coordinates coordinates) {
        CoordinatesDto dto = new CoordinatesDto();
        dto.id = coordinates.getId();
        dto.x = coordinates.getX();
        dto.y = coordinates.getY();
        return dto;
    }

    public static DisciplineDto toDiscipline(Discipline discipline) {
        DisciplineDto dto = new DisciplineDto();
        dto.id = discipline.getId();
        dto.name = discipline.getName();
        dto.lectureHours = discipline.getLectureHours();
        dto.selfStudyHours = discipline.getSelfStudyHours();
        dto.labsCount = discipline.getLabsCount();
        return dto;
    }

    public static PersonDto toPerson(Person person) {
        PersonDto dto = new PersonDto();
        dto.id = person.getId();
        dto.name = person.getName();
        dto.eyeColor = person.getEyeColor() == null ? null : person.getEyeColor().name();
        dto.hairColor = person.getHairColor() == null ? null : person.getHairColor().name();
        dto.location = person.getLocation() == null ? null : toLocation(person.getLocation());
        dto.weight = person.getWeight();
        dto.nationality = person.getNationality() == null ? null : person.getNationality().name();
        return dto;
    }

    public static LocationDto toLocation(Location location) {
        LocationDto dto = new LocationDto();
        dto.id = location.getId();
        dto.x = location.getX();
        dto.y = location.getY();
        dto.z = location.getZ();
        return dto;
    }

    public static IdName toIdName(Discipline discipline) {
        IdName dto = new IdName();
        dto.id = discipline.getId();
        dto.name = discipline.getName();
        return dto;
    }

}
