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

## Context map

```text
                         BFF Web
                            │
                            │ experiencias y composición
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              mvflix-movies: Media Catalog                   │
│                                                             │
│  ┌─────────────────┐       ┌────────────────────────────┐   │
│  │ Catalog (core)  │◄──────│ Library Ingestion         │   │
│  │ Movie           │ crea  │ MediaAsset / scan / match │   │
│  └────────┬────────┘       └────────────────────────────┘   │
│           │                                                 │
│           │ aplica metadata                                 │
│           ▼                                                 │
│  ┌─────────────────────────┐                                │
│  │ Metadata Enrichment     │                                │
│  │ búsqueda y vinculación  │                                │
│  └─────────────────────────┘                                │
└───────────┬──────────────────┬──────────────────┬────────────┘
            │ refs. lógicas    │ identidad JWT    │ metadata externa
            ▼                  ▼                  ▼
     mvflix-storage     authorization/users     TMDB
```

Las flechas expresan colaboración, no acceso a tablas. Cada microservicio
mantiene la propiedad de sus datos.

## Módulos internos

### Catalog

Módulo núcleo del bounded context. Es propietario del agregado `Movie`, su
metadata editorial, ciclo de vida, tipo, visibilidad, compartición y referencias
lógicas a contenido reproducible.

### Library Ingestion

Capacidad de soporte para descubrir archivos y convertirlos en elementos del
catálogo. `MediaAsset` es su Aggregate Root. La identificación es un caso de uso
de Application que coordina `MediaAsset` y `Movie`; ninguna de las dos entidades
de dominio conoce la operación reactiva ni la transacción.

### Metadata Enrichment

Capacidad de soporte del catálogo. Define el puerto `MetadataSource` desde el
lado consumidor y aplica al agregado `Movie` la metadata obtenida. TMDB es solo
un adapter de infraestructura y no define el modelo del dominio.

## Reglas de dependencia internas

1. El dominio de un módulo no depende de controllers, DTOs, adapters de base de
   datos, clientes HTTP ni configuración Spring.
2. Los controllers invocan casos de uso o servicios de consulta de Application;
   no acceden directamente a repositorios.
3. La colaboración entre `Library Ingestion` y `Catalog` se coordina en
   Application. Mientras ambos necesiten consistencia fuerte pueden compartir
   una transacción local y la misma base de datos.
4. Un módulo no escribe directamente las tablas privadas de otro módulo. Los
   accesos cruzados se expresan mediante un contrato definido por el consumidor.
5. `Metadata Enrichment` puede depender del modelo público de `Catalog`; Catalog
   no depende del adapter TMDB.
6. Los tipos compartidos se limitan a conceptos de negocio publicados, como la
   identidad de un elemento del catálogo. No se creará un paquete `shared` para
   acumular utilidades o DTOs sin dueño.

## Estrategia de extracción

La separación será incremental y conservará el mismo artefacto desplegable:

1. proteger las reglas actuales con tests de arquitectura;
2. mover una vertical slice completa de Library Ingestion (dominio,
   Application, persistencia y API);
3. reemplazar dependencias cruzadas sobre repositorios por contratos explícitos
   cuando exista una colaboración real;
4. evaluar otro despliegue o consistencia eventual solo si aparecen necesidades
   operativas independientes, escalado distinto o autonomía de equipos.

## Consecuencias

- No se crearán microservicios adicionales todavía.
- Los controllers no accederán directamente a repositorios.
- Movies no accederá a tablas de Storage.
- La autorización del catálogo seguirá siendo responsabilidad de Movies.
- Los cambios serán verticales e incrementales.
- Los módulos internos no implican nuevos microservicios ni eventos distribuidos.
