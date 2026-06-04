package com.kep.permission.api;

public record Subject(String type, long id) {

    public static Subject user(long id) { return new Subject("USER", id); }

    public static Subject orgUnit(long id) { return new Subject("ORG_UNIT", id); }
}
