package com.kep.permission.api;

public record AclRow(long resourceId, Subject subject, String permission, String effect) {}
