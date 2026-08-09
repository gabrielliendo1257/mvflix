package com.guille.media.reproductor.users.domain.exceptions;

/** Downgrade bloqueado porque el uso real supera la cuota del plan pedido. */
public class DowngradeBlockedByUsageException extends RuntimeException {

    public DowngradeBlockedByUsageException(String message) {
        super(message);
    }
}