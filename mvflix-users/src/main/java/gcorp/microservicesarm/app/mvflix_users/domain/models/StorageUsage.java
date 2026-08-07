package gcorp.microservicesarm.app.mvflix_users.domain.models;

import lombok.Getter;
import lombok.ToString;

/**
 * Bytes que lleva ocupando el usuario
 */
@Getter
@ToString
public class StorageUsage {
    private long userCurrentBytesUsage;

    public StorageUsage(long userCurrentBytesUsage) {
        this.userCurrentBytesUsage = userCurrentBytesUsage;
    }

    public StorageUsage addBytes(long bytes) {
        return new StorageUsage(this.userCurrentBytesUsage + bytes);
    }

    public void dismissBytes(long bytes) {
        this.userCurrentBytesUsage = this.userCurrentBytesUsage - bytes;
    }

    public long remaining(StorageQuota storageQuota) {
        return storageQuota.getUserBytesQuota() - this.userCurrentBytesUsage;
    }
}
