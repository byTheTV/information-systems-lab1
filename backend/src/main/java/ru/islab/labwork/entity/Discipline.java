package ru.islab.labwork.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "discipline")
public class Discipline {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Название дисциплины не может быть пустым")
    @Column(nullable = false)
    private String name;

    @NotNull(message = "Количество лекционных часов обязательно")
    @Column(name = "lecture_hours", nullable = false)
    private Integer lectureHours;

    @Column(name = "self_study_hours", nullable = false)
    private long selfStudyHours;

    @Column(name = "labs_count", nullable = false)
    private int labsCount;

    @ManyToMany
    @JoinTable(
            name = "discipline_program",
            joinColumns = @JoinColumn(name = "discipline_id"),
            inverseJoinColumns = @JoinColumn(name = "lab_work_id")
    )
    private Set<LabWork> program = new HashSet<>();

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getLectureHours() {
        return lectureHours;
    }

    public void setLectureHours(Integer lectureHours) {
        this.lectureHours = lectureHours;
    }

    public long getSelfStudyHours() {
        return selfStudyHours;
    }

    public void setSelfStudyHours(long selfStudyHours) {
        this.selfStudyHours = selfStudyHours;
    }

    public int getLabsCount() {
        return labsCount;
    }

    public void setLabsCount(int labsCount) {
        this.labsCount = labsCount;
    }

    public Set<LabWork> getProgram() {
        return program;
    }
}
