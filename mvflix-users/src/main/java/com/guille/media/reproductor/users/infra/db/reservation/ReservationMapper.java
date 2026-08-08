package com.guille.media.reproductor.users.infra.db.reservation;

import java.util.UUID;

import org.mapstruct.Mapper;

import com.guille.media.reproductor.users.domain.models.StorageReservations;
import com.guille.media.reproductor.users.domain.models.UserId;

@Mapper(componentModel = "spring")
public interface ReservationMapper {
    ReservationEntity toEntity(StorageReservations domain);

    StorageReservations toDomain(ReservationEntity entity);

    /* ---------- UserId ---------- */

    default UserId map(UUID id) {
        return id == null ? null : new UserId(id);
    }

    default UUID map(UserId id) {
        return id == null ? null : id.value();
    }
}
