package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.util.UUID;

import org.springframework.data.domain.Persistable;

/**
 * JPA entity for the {@code candidatos} table (V5 schema).
 * Lives in the infrastructure layer only — never imported by domain.
 *
 * <p>V5 changes:
 * - {@code numeroOrden} column DROPPED (alphabetical ordering now handled by Java layer)
 * - {@code afiliacionPolitica} column DROPPED (internal election, not relevant)
 * - {@code funcionarioId} column ADDED (nullable FK → funcionarios.id)
 */
@Entity
@Table(name = "candidatos")
public class CandidatoEntity implements Persistable<UUID> {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Transient
    private boolean isNew = true;

    @Column(name = "eleccion_id", nullable = false, columnDefinition = "UUID")
    private UUID eleccionId;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "es_voto_en_blanco", nullable = false)
    private Boolean esVotoEnBlanco = false;

    @Column(name = "es_voto_nulo", nullable = false)
    private Boolean esVotoNulo = false;

    @Column(name = "funcionario_id")
    private Integer funcionarioId;

    @Column(name = "foto_url", columnDefinition = "TEXT")
    private String fotoUrl;

    @Column(name = "biografia", columnDefinition = "TEXT")
    private String biografia;

    @Column(name = "propuestas", columnDefinition = "TEXT")
    private String propuestas;

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

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public Boolean getEsVotoEnBlanco() { return esVotoEnBlanco; }
    public void setEsVotoEnBlanco(Boolean esVotoEnBlanco) { this.esVotoEnBlanco = esVotoEnBlanco; }

    public Boolean getEsVotoNulo() { return esVotoNulo; }
    public void setEsVotoNulo(Boolean esVotoNulo) { this.esVotoNulo = esVotoNulo; }

    public Integer getFuncionarioId() { return funcionarioId; }
    public void setFuncionarioId(Integer funcionarioId) { this.funcionarioId = funcionarioId; }

    public String getFotoUrl() { return fotoUrl; }
    public void setFotoUrl(String fotoUrl) { this.fotoUrl = fotoUrl; }

    public String getBiografia() { return biografia; }
    public void setBiografia(String biografia) { this.biografia = biografia; }

    public String getPropuestas() { return propuestas; }
    public void setPropuestas(String propuestas) { this.propuestas = propuestas; }
}
