package com.gcorp.service.app.mvflix_movies.infrastructure.tmdb;

import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.domain.enrichment.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.Credits;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.CrewMember;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieDetails;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieSummary;

import java.util.List;
import java.util.Optional;

/**
 * Traduce los DTOs crudos de TMDB a los VOs de dominio. La URL del poster se
 * compone con la base de imagenes (los paths de TMDB son relativos).
 */
public final class TmdbMovieMapper {

    private static final String POSTER_SIZE = "w500";
    private static final int MAX_CAST = 10;

    private final String imageBaseUrl;

    public TmdbMovieMapper(String imageBaseUrl) {
        this.imageBaseUrl = imageBaseUrl;
    }

    public Optional<ExternalMovieSearch> toSearch(MovieSummary summary) {
        if (summary == null) {
            return Optional.empty();
        }
        Integer year = extractYear(summary.releaseDate());
        return Optional.of(new ExternalMovieSearch(
                summary.id(),
                summary.title(),
                year,
                posterUrl(summary.posterPath()),
                summary.releaseDate(),
                summary.overview()));
    }

    public ExternalMovieDetail toDetail(MovieDetails details) {
        Credits credits = details.credits() == null ? null : details.credits();
        return new ExternalMovieDetail(
                details.id(),
                details.title(),
                details.originalTitle(),
                extractYear(details.releaseDate()),
                details.genres() == null
                        ? List.of()
                        : details.genres().stream().map(g -> g.name()).toList(),
                details.popularity(),
                details.runtime(),
                director(credits),
                cast(credits),
                details.overview(),
                posterUrl(details.posterPath()),
                details.releaseDate(),
                firstCountry(details),
                firstLanguage(details));
    }

    private Integer extractYear(String releaseDate) {
        if (releaseDate == null || releaseDate.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(releaseDate.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String posterUrl(String posterPath) {
        return posterPath == null || posterPath.isBlank()
                ? null
                : this.imageBaseUrl + "/" + POSTER_SIZE + posterPath;
    }

    private String director(Credits credits) {
        if (credits == null || credits.crew() == null) {
            return null;
        }
        return credits.crew().stream()
                .filter(member -> "Director".equals(member.job()))
                .findFirst()
                .map(CrewMember::name)
                .orElse(null);
    }

    private List<String> cast(Credits credits) {
        if (credits == null || credits.cast() == null) {
            return List.of();
        }
        return credits.cast().stream()
                .map(member -> member.name())
                .limit(MAX_CAST)
                .toList();
    }

    private String firstCountry(MovieDetails details) {
        if (details.productionCountries() == null || details.productionCountries().isEmpty()) {
            return null;
        }
        return details.productionCountries().get(0).name();
    }

    private String firstLanguage(MovieDetails details) {
        if (details.spokenLanguages() == null || details.spokenLanguages().isEmpty()) {
            return null;
        }
        return details.spokenLanguages().get(0).englishName();
    }
}