package co.com.votapp.ws.electoral.domain.exception;

import java.util.UUID;

/**
 * Thrown when an attempt is made to retrieve results or generate reports
 * for an election that is NOT in the {@code FINALIZADA} state.
 *
 * <p>Per spec: the system MUST ONLY provide results for elections in FINALIZADA state.
 * This is a pure domain exception — ZERO Spring or framework imports.
 */
public class ElectionNotFinalizedException extends RuntimeException {

    private final UUID electionId;

    public ElectionNotFinalizedException(UUID electionId) {
        super("Election " + electionId + " is not FINALIZADA. "
                + "Results are only available for elections in FINALIZADA state.");
        this.electionId = electionId;
    }

    public UUID getElectionId() {
        return electionId;
    }
}
