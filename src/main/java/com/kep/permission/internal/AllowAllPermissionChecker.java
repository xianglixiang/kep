package com.kep.permission.internal;

import com.kep.permission.api.Permission;
import com.kep.permission.api.PermissionChecker;
import org.springframework.stereotype.Service;

/** M0 占位实现：一律放行。M1 替换为真实判定。 */
@Service
class AllowAllPermissionChecker implements PermissionChecker {

    @Override
    public void check(Long userId, Long catalogNodeId, Permission required) {
        // M0：不拦截
    }

    @Override
    public boolean has(Long userId, Long catalogNodeId, Permission required) {
        return true;
    }
}
