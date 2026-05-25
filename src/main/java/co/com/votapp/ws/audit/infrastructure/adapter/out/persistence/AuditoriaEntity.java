package co.com.votapp.ws.audit.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * JPA entity for the {@code auditoria_eventos} table (MVP schema).
 * Lives in the infrastructure layer only — never imported by domain.
 */
@Entity
@Table(name = "auditoria_eventos")
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "tipo", nullable = false, length = 50)
    private String tipo;

    @Column(name = "eleccion_id", columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "candidato_id", columnDefinition = "UUID")
    private UUID candidatoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public UUID getEleccionId() { return eleccionId; }
    public void setEleccionId(UUID eleccionId) { this.eleccionId = eleccionId; }

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }

    public UUID getCandidatoId() { return candidatoId; }
    public void setCandidatoId(UUID candidatoId) { this.candidatoId = candidatoId; }

    public Map<String, Object> getMetadata() { return metadata; }
    public void setMetadata(Map<String, Object> metadata) { this.metadata = metadata; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
