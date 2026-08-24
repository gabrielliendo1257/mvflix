package com.guille.media.bff.experience.shell.web;

import com.guille.media.bff.experience.shell.application.GetShellContext;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;

/**
 * Shell: bootstrap transversal de la interfaz. Responde "¿quién está usando
 * la aplicación y qué puede hacer?" — sin rutas/labels (eso es Angular) y sin
 * reglas de Movies/Storage/Libraries.
 */
@Tag(name = "Shell", description = "Bootstrap de la UI: identidad, capacidades y resumen de actividad propia")
@RestController
@RequestMapping(value = "/web/shell", produces = MediaType.APPLICATION_JSON_VALUE)
public class ShellController {

    private final GetShellContext getShellContext;

    public ShellController(GetShellContext getShellContext) {
        this.getShellContext = getShellContext;
    }

    @Operation(summary = "Contexto de arranque de la app (anónimo o autenticado)")
    @GetMapping
    public Mono<ShellResponse> shell() {
        return this.getShellContext.execute().map(ShellResponse::from);
    }
}
