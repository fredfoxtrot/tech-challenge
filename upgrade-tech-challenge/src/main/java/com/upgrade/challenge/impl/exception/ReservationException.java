package com.upgrade.challenge.impl.exception;

public class ReservationException extends Exception{

    public ReservationException(Throwable e) {
        super(e);
    }

    public ReservationException(String message) {
        super(message);
    }
    public ReservationException(String message, Throwable e) {
        super(message,e);
    }
}
