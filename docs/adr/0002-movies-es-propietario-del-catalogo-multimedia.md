# ADR 0002: Movies es propietario del catálogo multimedia

Estado: Aceptado

## Contexto

mvflix-movies gestiona películas, otros tipos de media, metadata,
visibilidad, compartición, enriquecimiento e identificación de
archivos provenientes de bibliotecas.

Storage posee los objetos físicos, uploads, bibliotecas, filesystem
y mecanismos de reproducción.

## Decisión

mvflix-movies representa el bounded context Media Catalog.

Es propietario de:

- elementos del catálogo;
- metadata editorial;
- estado DRAFT/READY;
- tipo de contenido;
- owner;
- visibilidad y compartición;
- asociación lógica con medios reproducibles;
- identificación de MediaAssets;
- enriquecimiento de metadata.

No es propietario de:

- objetos físicos;
- buckets;
- filesystem;
- cuotas;
- URLs firmadas;
- streaming;
- usuarios;
- política de planes.

## Modelo inicial

- Movie: Aggregate Root.
- MovieMetadata: Value Object.
- MediaAsset: Aggregate Root de ingesta.
- Media: provisionalmente entidad asociada a Movie.
- MetadataSource: puerto hacia proveedores externos.

## Decisión de despliegue

Catálogo, ingesta y enriquecimiento permanecen en un único
microservicio. Inicialmente se separarán como módulos internos.

## Consecuencias

- No se crearán microservicios adicionales todavía.
- Los controllers no accederán directamente a repositorios.
- Movies no accederá a tablas de Storage.
- La autorización del catálogo seguirá siendo responsabilidad de Movies.
- Los cambios serán verticales e incrementales.