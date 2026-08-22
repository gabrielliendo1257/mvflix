package com.gcorp.service.app.mvflix_movies.infrastructure.tmdb;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.catalog.application.port.ExternalMovieDetail;
import com.gcorp.service.app.mvflix_movies.catalog.application.port.ExternalMovieSearch;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.CastMember;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.Credits;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.CrewMember;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.Genre;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieDetails;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.MovieSummary;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.ProductionCountry;
import com.gcorp.service.app.mvflix_movies.infrastructure.tmdb.TmdbResponses.SpokenLanguage;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

class TmdbMovieMapperTest {

    private final TmdbMovieMapper mapper = new TmdbMovieMapper("https://image.tmdb.org/t/p");

    @Test
    void searchMapsFirstSummaryWithPosterAndYear() {
        MovieSummary summary =
                new MovieSummary(274_003L, "Il colosso di Rodi", "Overview...", "1961-06-20",
                        "/xYZ.jpg");

        Optional<ExternalMovieSearch> search = this.mapper.toSearch(summary);

        assertThat(search).isPresent();
        assertThat(search.get().tmdbId()).isEqualTo(274_003L);
        assertThat(search.get().year()).isEqualTo(1961);
        assertThat(search.get().posterPath())
                .isEqualTo("https://image.tmdb.org/t/p/w500/xYZ.jpg");
        assertThat(search.get().releaseDate()).isEqualTo("1961-06-20");
    }

    @Test
    void emptySummaryYieldsEmptySearch() {
        assertThat(this.mapper.toSearch(null)).isEmpty();
    }

    @Test
    void detailMapsDirectorCastCountryLanguageAndDuration() {
        MovieDetails details =
                new MovieDetails(
                        274_003L,
                        "Il colosso di Rodi",
                        "Il colosso di Rodi",
                        "1961-06-20",
                        "Overview...",
                        "/xYZ.jpg",
                        3.2,
                        128,
                        List.of(new Genre("Adventure"), new Genre("Action")),
                        List.of(new SpokenLanguage("Italian")),
                        List.of(new ProductionCountry("Italy")),
                        new Credits(
                                List.of(new CastMember("Rory Calhoun"), new CastMember("Lea Massari")),
                                List.of(
                                        new CrewMember("Sergio Leone", "Director"),
                                        new CrewMember("Otro", "Writer"))));

        ExternalMovieDetail detail = this.mapper.toDetail(details);

        assertThat(detail.director()).isEqualTo("Sergio Leone");
        assertThat(detail.cast()).containsExactly("Rory Calhoun", "Lea Massari");
        assertThat(detail.country()).isEqualTo("Italy");
        assertThat(detail.language()).isEqualTo("Italian");
        assertThat(detail.genres()).containsExactly("Adventure", "Action");
        assertThat(detail.year()).isEqualTo(1961);
        assertThat(detail.popularity()).isEqualTo(3.2);
        assertThat(detail.posterPath()).isEqualTo("https://image.tmdb.org/t/p/w500/xYZ.jpg");
    }

    @Test
    void detailWithoutCreditsIsNullSafe() {
        MovieDetails details =
                new MovieDetails(274_003L, "T", null, null, null, null, 0, 0, null, null, null,
                        null);

        ExternalMovieDetail detail = this.mapper.toDetail(details);

        assertThat(detail.director()).isNull();
        assertThat(detail.cast()).isEmpty();
        assertThat(detail.country()).isNull();
        assertThat(detail.language()).isNull();
        assertThat(detail.year()).isNull();
    }
}