package com.gcorp.service.app.mvflix_movies.presenter.api.dto;

import java.util.List;

/** Reemplazo de la lista de usuarios con quienes se comparte una pelicula. */
public record UpdateSharesRequest(List<String> usernames) {}