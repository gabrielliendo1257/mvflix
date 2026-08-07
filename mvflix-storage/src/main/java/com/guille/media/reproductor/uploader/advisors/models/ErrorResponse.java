package com.guille.media.reproductor.uploader.advisors.models;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ErrorResponse {
    private Integer code;
    private String message;
    private LocalDateTime timestamp;
}
