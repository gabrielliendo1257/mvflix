package com.guille.media.bff.app.dto;

/** Estado de un trabajo de visibilidad en lote para el progreso en la UI. */
public record BulkVisibilityJobDto(
        String jobId,
        String status,
        int total,
        int done,
        int failed) {

    public static BulkVisibilityJobDto running(
            String jobId, int total, int done, int failed) {
        return new BulkVisibilityJobDto(jobId, "RUNNING", total, done, failed);
    }

    public static BulkVisibilityJobDto done(
            String jobId, int total, int done, int failed) {
        return new BulkVisibilityJobDto(jobId, "DONE", total, done, failed);
    }
}