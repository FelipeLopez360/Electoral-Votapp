package co.com.votapp.ws.audit.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Domain aggregate for an audit event (MVP schema: auditoria_eventos).
 * No Spring or JPA annotations — pure Java.
 */
public class AuditoriaEvento {

    private final String tipo;
    private final UUID eleccionId;
    private final Integer funcionarioId;   // nullable — VOTE_ACCEPTED does not include funcionarioId
    private final UUID candidatoId;     // nullable
    private final Map<String, Object> metadata;
    private final Instant createdAt;

    public AuditoriaEvento(String tipo,
                           UUID eleccionId,
                           Integer funcionarioId,
                           UUID candidatoId,
                           Map<String, Object> metadata,
                           Instant createdAt) {
        if (tipo == null || tipo.isBlank())
            throw new IllegalArgumentException("tipo must not be blank");
        this.tipo = tipo;
        this.eleccionId = eleccionId;
        this.funcionarioId = funcionarioId;
        this.candidatoId = candidatoId;
        this.metadata = metadata;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
    }

    public String getTipo() { return tipo; }
    public UUID getEleccionId() { return eleccionId; }
    public Integer getFuncionarioId() { return funcionarioId; }
    public UUID getCandidatoId() { return candidatoId; }
    public Map<String, Object> getMetadata() { return metadata; }
    public Instant getCreatedAt() { return createdAt; }
}
