package com.guille.media.bff.experience.shell.application;

/** Usuario visible para la UI. displayName/avatarUrl llegan cuando existan. */
public record ShellUser(
    String id,
    String username,
    String displayName,
    String email,
    String avatarUrl) {}
