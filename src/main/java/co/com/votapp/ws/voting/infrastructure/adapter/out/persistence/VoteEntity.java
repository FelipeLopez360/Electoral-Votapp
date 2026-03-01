package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

/**
 * Entidad JPA para la tabla {@code votos}.
 *
 * <p>Nota arquitectural: esta clase vive en la capa de infraestructura.
 * NO debe importarse ni usarse en ninguna clase del dominio.
 *
 * <p>Mapeo al esquema {@code test_votaappdb.votos}:
 * <ul>
 *   <li>{@code id}            — SERIAL PRIMARY KEY  → Integer (auto-generado por BD)</li>
 *   <li>{@code uuid}          — UUID                → UUID (generado por BD via uuid-ossp)</li>
 *   <li>{@code eleccion_id}   — INTEGER             → Integer</li>
 *   <li>{@code candidato_id}  — INTEGER             → Integer</li>
 *   <li>{@code categoria_id}  — INTEGER             → Integer</li>
 *   <li>{@code token_id}      — UUID NOT NULL UNIQUE → UUID</li>
 *   <li>{@code timestamp_voto}— TIMESTAMP            → Instant</li>
 * </ul>
 */
@Entity
@Table(name = "votos", schema = "test_votaappdb")
public class VoteEntity {

    /** SERIAL (INTEGER) — generado por la secuencia de la BD, NO por Hibernate. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** UUID generado por la función {@code generate_uuid()} (uuid-ossp). */
    @Column(name = "uuid", updatable = false, insertable = false)
    private UUID uuid;

    @Column(name = "eleccion_id", nullable = false)
    private Integer electionId;

    @Column(name = "candidato_id", nullable = false)
    private Integer candidateId;

    @Column(name = "categoria_id", nullable = false)
    private Integer categoryId;

    /** UUID del token usado para votar (referencia lógica al UUID de tokens_votacion). */
    @Column(name = "token_id", nullable = false, unique = true, columnDefinition = "UUID")
    private UUID tokenId;

    @Column(name = "timestamp_voto", nullable = false)
    private Instant castAt;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public Integer getElectionId() {
        return electionId;
    }

    public void setElectionId(Integer electionId) {
        this.electionId = electionId;
    }

    public Integer getCandidateId() {
        return candidateId;
    }

    public void setCandidateId(Integer candidateId) {
        this.candidateId = candidateId;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Integer categoryId) {
        this.categoryId = categoryId;
    }

    public UUID getTokenId() {
        return tokenId;
    }

    public void setTokenId(UUID tokenId) {
        this.tokenId = tokenId;
    }

    public Instant getCastAt() {
        return castAt;
    }

    public void setCastAt(Instant castAt) {
        this.castAt = castAt;
    }
}
