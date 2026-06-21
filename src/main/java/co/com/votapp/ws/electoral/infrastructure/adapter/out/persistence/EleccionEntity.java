package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.domain.Persistable;

/**
 * JPA entity for the {@code elecciones} table (MVP schema).
 * Lives in the infrastructure layer only — never imported by domain.
 *
 * <p>Implements {@link Persistable} so Spring Data JPA detects isNew() correctly.
 * Without this, Hibernate 7's save() delegates to merge() for entities with
 * non-null IDs and throws StaleObjectStateException when @Version is absent.
 */
@Entity
@Table(name = "elecciones")
public class EleccionEntity implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "estado", nullable = false, length = 20)
    private String estado;

    @Column(name = "fecha_inicio", nullable = false)
    private Instant fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private Instant fechaFin;

    @Column(name = "permite_voto_blanco", nullable = false)
    private boolean permiteVotoBlanco = true;

    @Column(name = "max_votos_por_elector", nullable = false)
    private int maxVotosPorElector = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

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

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Instant getFechaInicio() { return fechaInicio; }
    public void setFechaInicio(Instant fechaInicio) { this.fechaInicio = fechaInicio; }

    public Instant getFechaFin() { return fechaFin; }
    public void setFechaFin(Instant fechaFin) { this.fechaFin = fechaFin; }

    public boolean isPermiteVotoBlanco() { return permiteVotoBlanco; }
    public void setPermiteVotoBlanco(boolean permiteVotoBlanco) { this.permiteVotoBlanco = permiteVotoBlanco; }

    public int getMaxVotosPorElector() { return maxVotosPorElector; }
    public void setMaxVotosPorElector(int maxVotosPorElector) { this.maxVotosPorElector = maxVotosPorElector; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
