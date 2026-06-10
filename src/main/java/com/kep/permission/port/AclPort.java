package com.kep.permission.port;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Permission;
import com.kep.permission.api.Subject;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public interface AclPort {

    List<AclRow> findHits(String tenantId, Collection<Long> resourceIds,
                          Set<Subject> subjects, Permission permission);
}
