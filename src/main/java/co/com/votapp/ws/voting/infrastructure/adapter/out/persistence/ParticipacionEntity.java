package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code participacion_electoral} table.
 *
 * <p>Records THAT a funcionario voted in an election — never WHAT they voted for.
 * Used to enforce the one-vote-per-election invariant at the DB level.
 * Lives in the infrastructure layer only — never imported by domain.
 */
@Entity
@Table(
        name = "participacion_electoral",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_participacion_eleccion_funcionario",
                columnNames = {"eleccion_id", "funcionario_id"}
        )
)
public class ParticipacionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "eleccion_id", nullable = false, columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "funcionario_id", nullable = false)
    private Integer funcionarioId;

    @Column(name = "voted_at", nullable = false)
    private Instant votedAt;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getEleccionId() { return eleccionId; }
    public void setEleccionId(UUID eleccionId) { this.eleccionId = eleccionId; }

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }

    public Instant getVotedAt() { return votedAt; }
    public void setVotedAt(Instant votedAt) { this.votedAt = votedAt; }
}
