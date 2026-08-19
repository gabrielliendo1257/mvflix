package com.guille.media.bff.app.dto;

/** Body del alta de biblioteca: un path del filesystem del operador. */
public record RegisterLibraryRequest(String rootPath) {}