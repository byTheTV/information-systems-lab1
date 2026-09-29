package ru.islab.labwork.dto;

import java.util.List;

public class DeleteInfo {
    public boolean requiresReplacement;
    public long referenceCount;
    public String message;
    public List<IdName> programs;
}
