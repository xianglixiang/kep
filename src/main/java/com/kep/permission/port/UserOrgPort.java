package com.kep.permission.port;

import java.util.Set;

public interface UserOrgPort {

    /** 返回 user 所属的所有 org_unit 及其祖先 org_unit 的 id 集合（含直属）。 */
    Set<Long> ancestorOrgIds(long userId);
}
