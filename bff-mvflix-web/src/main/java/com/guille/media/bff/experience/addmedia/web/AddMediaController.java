package com.guille.media.bff.experience.addmedia.web;

import com.guille.media.bff.app.service.WebSessionService;
import com.guille.media.bff.experience.addmedia.application.CancelAddMedia;
import com.guille.media.bff.experience.addmedia.application.CompleteProcessAddMedia;
import com.guille.media.bff.experience.addmedia.application.PreviewMovieCandidate;
import com.guille.media.bff.experience.addmedia.application.SearchMovieCandidates;
import com.guille.media.bff.experience.addmedia.application.StartAddMedia;
import com.guille.media.bff.experience.addmedia.application.port.AddMediaProcessRepository;
import com.guille.media.bff.experience.addmedia.model.AddMediaId;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guille.media.bff.app.dto.MovieEnrichmentPreviewDto;
import com.guille.media.bff.app.dto.MovieEnrichmentSearchDto;
import org.springframework.web.server.ResponseStatusException;

import jakarta.validation.Valid;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Experiencia Add Media: el front dice "añade este contenido" y no aprende
 * cómo están divididos los microservicios.
 */
@RestController
@RequestMapping(value = "/web/add-media", produces = MediaType.APPLICATION_JSON_VALUE)
public class AddMediaController {

  private final SearchMovieCandidates searchMovieCandidates;
  private final PreviewMovieCandidate previewMovieCandidate;
  private final StartAddMedia startAddMedia;
  private final CompleteProcessAddMedia completeProcess;
  private final CancelAddMedia cancelAddMedia;
  private final AddMediaProcessRepository processes;
  private final WebSessionService session;

  public AddMediaController(
      SearchMovieCandidates searchMovieCandidates,
      PreviewMovieCandidate previewMovieCandidate,
      StartAddMedia startAddMedia,
      CompleteProcessAddMedia completeProcess,
      CancelAddMedia cancelAddMedia,
      AddMediaProcessRepository processes,
      WebSessionService session) {
    this.searchMovieCandidates = searchMovieCandidates;
    this.previewMovieCandidate = previewMovieCandidate;
    this.startAddMedia = startAddMedia;
    this.completeProcess = completeProcess;
    this.cancelAddMedia = cancelAddMedia;
    this.processes = processes;
    this.session = session;
  }

  @GetMapping("/candidates")
  public Flux<MovieEnrichmentSearchDto> candidates(
      @RequestParam String query, @RequestParam(required = false) Integer year) {
    return this.searchMovieCandidates.search(query, year);
  }

  @GetMapping("/candidates/{providerId}")
  public Mono<MovieEnrichmentPreviewDto> candidate(@PathVariable Long providerId) {
    return this.previewMovieCandidate.preview(providerId);
  }

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<AddMediaView>> start(
      @Valid @RequestBody StartAddMediaRequest request) {
    return this.ownerSubject()
        .flatMap(owner -> this.startAddMedia.handle(owner, request))
        .map(view -> ResponseEntity.status(HttpStatus.CREATED).body(view));
  }

  @GetMapping("/{addMediaId}")
  public Mono<AddMediaView> status(
      @PathVariable String addMediaId) {
    return this.ownerSubject()
        .flatMap(owner ->
            this.processes
                .findById(new AddMediaId(addMediaId))
                .filter(process -> process.ownedBy(owner))
                .switchIfEmpty(Mono.error(
                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Proceso no encontrado")))
                .map(AddMediaView::from));
  }

  /**
   * Cierre del alta. 200 si quedó READY, 202 VERIFYING_UPLOAD si storage aún
   * verifica, 409 ante veredicto definitivo (rollback ya ejecutado).
   */
  @PostMapping(value = "/{addMediaId}/complete", consumes = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<AddMediaView>> complete(
      @PathVariable String addMediaId,
      @RequestBody(required = false) CompleteSizeRequest request) {
    Long sizeBytes = request == null ? null : request.sizeBytes();
    return this.ownerSubject()
        .flatMap(owner -> this.completeProcess.handle(owner, addMediaId, sizeBytes))
        .map(view -> view.phase() == com.guille.media.bff.experience.addmedia.model.AddMediaPhase.READY
            ? ResponseEntity.ok(view)
            : ResponseEntity.accepted().body(view));
  }

  /** Cancelación del proceso con compensaciones acotadas. */
  @PostMapping("/{addMediaId}/cancel")
  public Mono<AddMediaView> cancel(@PathVariable String addMediaId) {
    return this.ownerSubject()
        .flatMap(owner -> this.cancelAddMedia.handle(owner, addMediaId));
  }

  public record CompleteSizeRequest(Long sizeBytes) {}

  /**
   * Sujeto propietario del proceso. Fallback "sandbox" SOLO para el perfil sin
   * auth: en producción el subject viene del token de sesión OAuth2 y un
   * anónimo recibe 401 por la cadena de seguridad antes de llegar aquí.
   */
  private Mono<String> ownerSubject() {
    return this.session.currentSubject().defaultIfEmpty("sandbox");
  }
}
