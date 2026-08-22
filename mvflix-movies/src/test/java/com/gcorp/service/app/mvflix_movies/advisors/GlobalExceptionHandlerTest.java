package com.gcorp.service.app.mvflix_movies.advisors;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.advisors.models.ErrorResponse;
import com.gcorp.service.app.mvflix_movies.domain.mediaasset.MediaAssetNotFoundException;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void mapsMissingMediaAssetToNotFound() {
        ResponseEntity<ErrorResponse> response = this.handler.mediaAssetNotFound(
                new MediaAssetNotFoundException("Media asset not found: 10"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(
                        404, "MEDIA_ASSET_NOT_FOUND", "Media asset not found: 10"));
    }

    @Test
    void doesNotExposeUnexpectedExceptionDetails() {
        ResponseEntity<ErrorResponse> response = this.handler.globalError(
                new IllegalStateException("password=secret"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody())
                .isEqualTo(new ErrorResponse(
                        500, "INTERNAL_SERVER_ERROR", "Unexpected server error"));
    }
}
