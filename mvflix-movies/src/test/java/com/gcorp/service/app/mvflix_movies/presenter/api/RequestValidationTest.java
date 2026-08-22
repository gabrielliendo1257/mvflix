package com.gcorp.service.app.mvflix_movies.presenter.api;

import static org.assertj.core.api.Assertions.assertThat;

import com.gcorp.service.app.mvflix_movies.domain.movie.MovieVisibility;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.BulkVisibilityRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CompleteMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.CreateMovieRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.IdentifyAssetRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.ScanLibraryRequest;
import com.gcorp.service.app.mvflix_movies.presenter.api.dto.UpdateVisibilityRequest;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

class RequestValidationTest {

    private static final ValidatorFactory VALIDATOR_FACTORY =
            Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = VALIDATOR_FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        VALIDATOR_FACTORY.close();
    }

    @Test
    void createRequiresATitle() {
        CreateMovieRequest request = new CreateMovieRequest(
                " ", null, null, null, null, null, null, null, null,
                null, null, null, null, null, null);

        assertThat(propertiesOf(request)).containsExactly("title");
    }

    @Test
    void completeRequiresAValidObjectReference() {
        CompleteMovieRequest request = new CompleteMovieRequest(0L, " ");

        assertThat(propertiesOf(request)).containsExactlyInAnyOrder("objectId", "objectKey");
    }

    @Test
    void identifyRequiresTitleAndPositiveProviderId() {
        IdentifyAssetRequest request = new IdentifyAssetRequest("", -1L, null);

        assertThat(propertiesOf(request)).containsExactlyInAnyOrder("title", "tmdbId");
    }

    @Test
    void scanValidatesNestedFileData() {
        ScanLibraryRequest request = new ScanLibraryRequest(List.of(
                new ScanLibraryRequest.ScannedFileItem(" ", -1L, "")));

        assertThat(propertiesOf(request))
                .containsExactlyInAnyOrder(
                        "files[0].relativePath", "files[0].size", "files[0].mimeType");
    }

    @Test
    void visibilityCommandsRequireVisibilityAndPositiveIds() {
        BulkVisibilityRequest bulk = new BulkVisibilityRequest(
                List.of(0L), List.of(-1L), null, List.of());

        assertThat(propertiesOf(bulk))
                .containsExactlyInAnyOrder(
                        "movieIds[0].<list element>",
                        "libraryIds[0].<list element>",
                        "visibility");
        assertThat(propertiesOf(new UpdateVisibilityRequest(null)))
                .containsExactly("visibility");
        assertThat(propertiesOf(new UpdateVisibilityRequest(MovieVisibility.PRIVATE)))
                .isEmpty();
    }

    private static Set<String> propertiesOf(Object request) {
        return VALIDATOR.validate(request).stream()
                .map(ConstraintViolation::getPropertyPath)
                .map(Object::toString)
                .collect(java.util.stream.Collectors.toSet());
    }
}
