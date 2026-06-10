package com.kep.permission.port;

import com.kep.permission.api.AclRow;
import com.kep.permission.api.Permission;
import com.kep.permission.api.Subject;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@Profile("!local-mock")
class JpaAclPort implements AclPort {

    private final JdbcTemplate jdbc;

    JpaAclPort(JdbcTemplate jdbc) { this.jdbc = jdbc; }

    @Override
    public List<AclRow> findHits(String tenantId, Collection<Long> resourceIds,
                                 Set<Subject> subjects, Permission permission) {
        if (resourceIds.isEmpty() || subjects.isEmpty()) return List.of();

        Set<Long> userIds = new HashSet<>();
        Set<Long> orgIds = new HashSet<>();
        for (Subject s : subjects) {
            if ("USER".equals(s.type())) userIds.add(s.id());
            else if ("ORG_UNIT".equals(s.type())) orgIds.add(s.id());
        }

        StringBuilder sql = new StringBuilder(
            "SELECT resource_id, subject_type, subject_id, permission, effect " +
            "FROM acl WHERE tenant_id = ? AND resource_id IN (");
        sql.append(String.join(",", Collections.nCopies(resourceIds.size(), "?")));
        sql.append(") AND permission = ? AND (");
        List<Object> args = new ArrayList<>();
        args.add(tenantId);
        args.addAll(resourceIds);
        args.add(permission.name());
        List<String> disjuncts = new ArrayList<>();
        if (!userIds.isEmpty()) {
            disjuncts.add("(subject_type='USER' AND subject_id IN (" +
                String.join(",", Collections.nCopies(userIds.size(), "?")) + "))");
            args.addAll(userIds);
        }
        if (!orgIds.isEmpty()) {
            disjuncts.add("(subject_type='ORG_UNIT' AND subject_id IN (" +
                String.join(",", Collections.nCopies(orgIds.size(), "?")) + "))");
            args.addAll(orgIds);
        }
        sql.append(String.join(" OR ", disjuncts)).append(")");

        return jdbc.query(sql.toString(), (rs, i) -> new AclRow(
            rs.getLong("resource_id"),
            new Subject(rs.getString("subject_type"), rs.getLong("subject_id")),
            rs.getString("permission"),
            rs.getString("effect")
        ), args.toArray());
    }
}
