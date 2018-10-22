package com.kudoji.kman.enums;

/**
 * What account to take into consideration
 *
 * Used is Transaction model
 */
public enum AccountTake {
    /**
     * take account_to
     */
    TO,
    /**
     * take account_from
     */
    FROM,
    /**
     * take both account_to and account_from
     */
    BOTH
}
