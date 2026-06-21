package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code votos} table (MVP schema).
 * No funcionario_id — anonymity is guaranteed by design.
 * Lives in the infrastructure layer only — never imported by domain.
 */
@Entity
@Table(name = "votos")
public class VoteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "eleccion_id", nullable = false, columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "candidato_id", nullable = false, columnDefinition = "UUID")
    private UUID candidatoId;

    @Column(name = "token_id", nullable = false, columnDefinition = "UUID")
    private UUID tokenId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEleccionId() { return eleccionId; }
    public void setEleccionId(UUID eleccionId) { this.eleccionId = eleccionId; }

    public UUID getCandidatoId() { return candidatoId; }
    public void setCandidatoId(UUID candidatoId) { this.candidatoId = candidatoId; }

    public UUID getTokenId() { return tokenId; }
    public void setTokenId(UUID tokenId) { this.tokenId = tokenId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
