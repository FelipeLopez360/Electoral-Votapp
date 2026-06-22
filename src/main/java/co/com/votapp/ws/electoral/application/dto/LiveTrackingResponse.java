package co.com.votapp.ws.electoral.application.dto;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Read-only DTO for the live-tracking dashboard endpoint
 * ({@code GET /api/v1/dashboard/live-tracking}).
 *
 * <p>Returns per-election participation progress for all currently ACTIVA elections.
 * Serialized directly to JSON by the REST layer. No Spring or JPA imports — pure Java
 * record per hexagonal architecture rules.
 *
 * @param elections list of active elections with their participation progress
 */
public record LiveTrackingResponse(List<LiveTrackingItem> elections) {

    public LiveTrackingResponse {
        Objects.requireNonNull(elections, "elections list is required");
        elections = List.copyOf(elections);
    }

    /**
     * Per-election participation tracking entry.
     *
     * @param id                   the election UUID
     * @param title                the election name (nombre)
     * @param totalCastVotes       number of participacion_electoral records for this election
     * @param totalEligibleVoters  census count if &gt; 0, otherwise global active voter count
     */
    public record LiveTrackingItem(
            UUID id,
            String title,
            long totalCastVotes,
            long totalEligibleVoters
    ) {
        public LiveTrackingItem {
            Objects.requireNonNull(id, "id is required");
            Objects.requireNonNull(title, "title is required");
        }
    }
}
