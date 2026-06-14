package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.electoral.domain.model.CensoEntry;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import org.springframework.data.domain.Persistable;

import java.time.Instant;
import java.util.UUID;

/**
 * JPA entity for the {@code censo_electoral} table.
 *
 * <p>Lives in the infrastructure layer only — NEVER imported by domain.
 *
 * <p>Implements {@link Persistable} so Spring Data JPA correctly distinguishes
 * inserts from updates for entities with pre-assigned UUIDs.
 */
@Entity
@Table(
        name = "censo_electoral",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_censo_eleccion_funcionario",
                columnNames = {"eleccion_id", "funcionario_id"}
        )
)
public class CensoEntity implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    /** Tracks whether this is a new entity (not yet persisted). */
    @jakarta.persistence.Transient
    private boolean isNew = true;

    @Column(name = "eleccion_id", nullable = false, columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "funcionario_id", nullable = false)
    private Integer funcionarioId;

    /**
     * FK to funcionarios(id) — the admin who added this entry.
     * Stored as INTEGER to match the DB schema. Nullable in MVP.
     */
    @Column(name = "agregado_por")
    private Integer agregadoPor;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ─── Persistable ─────────────────────────────────────────────────────────

    @Override
    public UUID getId() { return id; }

    @Override
    @jakarta.persistence.Transient
    public boolean isNew() { return isNew; }

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public void setId(UUID id) {
        this.id = id;
        if (id != null) this.isNew = false;
    }

    public UUID getEleccionId() { return eleccionId; }
    public void setEleccionId(UUID eleccionId) { this.eleccionId = eleccionId; }

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }

    public Integer getAgregadoPor() { return agregadoPor; }
    public void setAgregadoPor(Integer agregadoPor) { this.agregadoPor = agregadoPor; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    // ─── Domain mapping ───────────────────────────────────────────────────────

    /**
     * Convert this JPA entity to its domain representation.
     */
    public CensoEntry toDomain() {
        return new CensoEntry(id, eleccionId, funcionarioId, agregadoPor, createdAt);
    }

    /**
     * Create a new (unsaved) entity from a domain record.
     * {@code id} is left null so JPA generates it on insert.
     */
    public static CensoEntity fromDomain(CensoEntry entry) {
        CensoEntity entity = new CensoEntity();
        // Only set id if entry already has one (e.g., from a previous save)
        if (entry.id() != null) {
            entity.setId(entry.id());
        }
        entity.setEleccionId(entry.eleccionId());
        entity.setFuncionarioId(entry.funcionarioId());
        entity.setAgregadoPor(entry.agregadoPor());
        entity.setCreatedAt(entry.fechaAgregado() != null ? entry.fechaAgregado() : Instant.now());
        return entity;
    }
}
