package gcorp.microservicesarm.app.mvflix_users.api.dto;

import java.time.Instant;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ApiError {
    private int status;
    private String detail;
    private String instance;
    private Instant timestamp;
}