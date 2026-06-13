package co.com.votapp.ws.electoral.domain.exception;

import co.com.votapp.ws.electoral.domain.ElectionStatus;

import java.util.UUID;

/**
 * Thrown when an attempt is made to modify an election's census
 * while the election is NOT in {@link ElectionStatus#PROGRAMADA} state.
 *
 * <p>Only PROGRAMADA elections may have their census modified.
 * This is a pure domain exception — ZERO Spring or framework imports.
 */
public class ElectionNotModifiableException extends RuntimeException {

    private final UUID eleccionId;
    private final ElectionStatus currentStatus;

    public ElectionNotModifiableException(UUID eleccionId, ElectionStatus currentStatus) {
        super("Election " + eleccionId + " cannot be modified in state " + currentStatus
                + ". Only PROGRAMADA elections allow census modifications.");
        this.eleccionId = eleccionId;
        this.currentStatus = currentStatus;
    }

    public UUID getEleccionId() {
        return eleccionId;
    }

    public ElectionStatus getCurrentStatus() {
        return currentStatus;
    }
}
