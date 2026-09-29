package ru.islab.labwork.dto;

import java.util.List;

public class LabWorkDto {
    public Integer id;
    public String name;
    public CoordinatesDto coordinates;
    public String creationDate;
    public String description;
    public String difficulty;
    public DisciplineDto discipline;
    public long minimalPoint;
    public Integer tunedInWorks;
    public PersonDto author;
    public List<IdName> programs;
}
