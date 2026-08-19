package com.guille.media.bff.app.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class StreamTicketServiceTest {

  private final StreamTicketService service = new StreamTicketService("test-secret", 300);

  @Test
  void issuedTicketResolvesWithMovieAndJwt() {
    String ticket = this.service.issue(7L, "user-jwt-value");

    StreamTicket resolved = this.service.resolve(ticket);

    assertEquals(7L, resolved.movieId());
    assertEquals("user-jwt-value", resolved.userJwt());
  }

  @Test
  void tamperedTicketIsRejected() {
    String ticket = this.service.issue(7L, "user-jwt-value");
    String tampered = ticket.substring(0, ticket.length() - 2) + "xx";

    assertThrows(StreamTicketException.class, () -> this.service.resolve(tampered));
  }

  @Test
  void garbageTicketIsRejected() {
    assertThrows(StreamTicketException.class, () -> this.service.resolve("no-es-un-ticket"));
  }

  @Test
  void expiredTicketIsRejected() {
    StreamTicketService shortLived = new StreamTicketService("test-secret", -1);
    String ticket = shortLived.issue(7L, "user-jwt-value");

    assertThrows(StreamTicketException.class, () -> shortLived.resolve(ticket));
  }

  @Test
  void differentSecretIsRejected() {
    String ticket = new StreamTicketService("secret-a", 300).issue(7L, "jwt");

    assertThrows(StreamTicketException.class, () -> new StreamTicketService("secret-b", 300).resolve(ticket));
  }
}