package com.chroniccare.models;

import java.time.LocalDate;

public class HolidayInfo {
    private final LocalDate date;
    private final String localName;
    private final String name;

    public HolidayInfo(LocalDate date, String localName, String name) {
        this.date = date;
        this.localName = localName;
        this.name = name;
    }

    public LocalDate getDate() {
        return date;
    }

    public String getLocalName() {
        return localName;
    }

    public String getName() {
        return name;
    }
}
