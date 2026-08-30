# Activity

`mvflix-activity` es una proyección de lectura. Consume `PlaybackProgressed.v1`,
persiste su inbox y proyecta el estado de visionado en PostgreSQL. Playback es el
productor del evento; Activity no llama a Movies/Storage ni coordina sagas y no
expone endpoints de escritura.

Las consultas autenticadas por JWT están disponibles en `/api/v1/activity`.
