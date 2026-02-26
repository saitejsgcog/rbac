// file: src/main/java/com/gridinsight/backend/entity/AuthEventType.java
package com.gridinsight.backend.entity;

public enum AuthEventType {
    LOGIN_SUCCESS, LOGIN_FAILED, ACCOUNT_LOCKED,
    REFRESH_SUCCESS, REFRESH_REUSE_DETECTED,
    LOGOUT
}