package com.guille.media.reproductor.uploader.storage.domain.vos;

/**
 * Identifica el proveedor físico de almacenamiento utilizado por el sistema.
 *
 * <p>
 * Este enum pertenece al dominio y abstrae al negocio de detalles concretos
 * de infraestructura. El resto de la aplicación no necesita conocer SDKs ni
 * APIs
 * específicas; solo necesita saber qué proveedor respalda el almacenamiento.
 *
 * <p>
 * Ejemplos:
 * <ul>
 * <li>S3 -> Amazon S3</li>
 * <li>R2 -> Cloudflare R2</li>
 * <li>MINIO -> MinIO autohospedado o compatible con S3</li>
 * <li>LOCAL -> Sistema de archivos local</li>
 * </ul>
 */
public enum StorageProvider {

    /** Amazon S3. */
    S3,

    /** Cloudflare R2. */
    R2,

    /** MinIO o cualquier servicio compatible con la API de S3. */
    MINIO,

    /** Almacenamiento en disco local. */
    LOCAL,

    /** Google Cloud Storage. */
    GCS,

    /** Microsoft Azure Blob Storage. */
    AZURE_BLOB;

    /**
     * Indica si el proveedor es compatible con la API de Amazon S3.
     *
     * <p>
     * Esto es útil porque varios proveedores (por ejemplo MinIO y R2)
     * permiten reutilizar el mismo adaptador basado en el SDK de AWS.
     *
     * @return {@code true} si el proveedor puede utilizar un adaptador
     *         S3-compatible.
     */
    public boolean isS3Compatible() {
        return this == S3 || this == R2 || this == MINIO;
    }

    /**
     * Indica si el proveedor representa almacenamiento local en disco.
     *
     * @return {@code true} si el proveedor es {@link #LOCAL}.
     */
    public boolean isLocal() {
        return this == LOCAL;
    }

    /**
     * Devuelve un nombre legible para interfaces de usuario o logs.
     *
     * @return nombre amigable del proveedor.
     */
    public String displayName() {
        return switch (this) {
            case S3 -> "Amazon S3";
            case R2 -> "Cloudflare R2";
            case MINIO -> "MinIO";
            case LOCAL -> "Local Storage";
            case GCS -> "Google Cloud Storage";
            case AZURE_BLOB -> "Azure Blob Storage";
        };
    }
}
