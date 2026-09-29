package ru.islab.labwork.dto;

import java.util.List;

public class PageDto<T> {
    public List<T> items;
    public long total;
    public int page;
    public int size;
}
