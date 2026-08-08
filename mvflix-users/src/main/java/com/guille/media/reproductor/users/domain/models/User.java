package com.guille.media.reproductor.users.domain.models;

import com.guille.media.reproductor.users.domain.exceptions.ExceededQuotaException;
import com.guille.media.reproductor.users.domain.exceptions.UserDisabledException;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@Data
@ToString
@AllArgsConstructor
public class User {
    private UserId id;
    private Username username;
    private Email email;
    private Plan plan;
    private StorageQuota storageQuota;
    private StorageUsage storageUsed;
    private boolean enabled;

	public static User createNew(Username username, Email email) {
		Plan plan = Plan.FREE;

		return new User(
			new UserId(UUID.randomUUID()),
			username,
			email,
			plan,
			StorageQuota.getQuota(plan),
			new StorageUsage(0L),
			true
		);
	}

    public void consumeStorage(long bytes) {
		if (!this.canUpload(bytes)) {
			throw new UserDisabledException("User is disabled.");
		}

        StorageUsage nextUsage = this.storageUsed.addBytes(bytes);

        if (this.storageQuota.isExceeded(nextUsage)) {
            throw new ExceededQuotaException("Excede el storage permitido.");
        }

        this.storageUsed = nextUsage;
    }

    public void releaseStorage(long bytes) {
        this.getStorageUsed().dismissBytes(bytes);
    }

    public void changePlan(Plan plan) {
        this.plan = plan;
    }

    public boolean canUpload(long bytes) {
		return this.isEnabled();
	}
}