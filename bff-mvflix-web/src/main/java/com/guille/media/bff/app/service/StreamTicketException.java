package com.guille.media.bff.app.service;

/** Ticket de stream inválido, expirado o de otra movie. */
public class StreamTicketException extends RuntimeException {

  public StreamTicketException(String message) {
    super(message);
  }
}