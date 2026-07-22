package com.apargo.waba.domain.enums;

public enum PhoneNumberStatus {

    /**
     * Phone can be used.
     */
    ACTIVE,

    /**
     * Waiting for activation.
     */
    PENDING,

    /**
     * Registration failed.
     */
    REGISTRATION_FAILED,

    /**
     * Disabled by our application.
     */
    DISABLED,

    /**
     * Blocked.
     */
    BLOCKED

}