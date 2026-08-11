package com.chuangyi.myofficedevice.user;

public enum UserRole {
    USER,
    ADMIN,
    SUPER_ADMIN;

    public boolean canEditTopology() {
        return this == ADMIN || this == SUPER_ADMIN;
    }

    public boolean canManageSystem() {
        return this == SUPER_ADMIN;
    }
}
