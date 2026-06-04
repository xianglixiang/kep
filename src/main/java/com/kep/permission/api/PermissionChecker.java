package com.kep.permission.api;

/**
 * 全系统唯一鉴权入口。各上层模块只经此判定，不自写权限逻辑。
 * M0 为桩实现；M1 落地三层鉴权（租户隔离 / 目录 ACL 就近优先 / 读写分离）。
 */
public interface PermissionChecker {

    void check(Long userId, Long catalogNodeId, Permission required);

    boolean has(Long userId, Long catalogNodeId, Permission required);
}
