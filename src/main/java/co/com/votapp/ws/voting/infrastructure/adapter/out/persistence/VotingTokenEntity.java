package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

/**
 * JPA entity for the {@code tokens_votacion} table.
 *
 * <p>Stores hashed voting tokens only — the raw secret is ephemeral and NEVER persisted.
 * Lives in the infrastructure layer only — never imported by domain.
 */
@Entity
@Table(
        name = "tokens_votacion",
        indexes = {
                @Index(name = "idx_tokens_votacion_token_hash", columnList = "token_hash"),
                @Index(name = "idx_tokens_votacion_eleccion_funcionario", columnList = "eleccion_id,funcionario_id")
        }
)
public class VotingTokenEntity implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "eleccion_id", nullable = false, columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "funcionario_id", nullable = false)
    private Integer funcionarioId;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "used_ip", length = 45)
    private String usedIp;

    @Column(name = "user_agent", length = 512)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // ─── Persistable ─────────────────────────────────────────────────────────

    @Override
    public UUID getId() { return id; }

    @Override
    @Transient
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

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }

    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }

    public String getUsedIp() { return usedIp; }
    public void setUsedIp(String usedIp) { this.usedIp = usedIp; }

    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
