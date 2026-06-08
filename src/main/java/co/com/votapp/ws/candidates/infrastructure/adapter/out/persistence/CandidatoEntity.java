package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.util.UUID;

import org.springframework.data.domain.Persistable;

/**
 * JPA entity for the {@code candidatos} table (MVP schema).
 * Lives in the infrastructure layer only — never imported by domain.
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

    @Column(name = "numero_orden", nullable = false)
    private Integer numeroOrden;

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

    public Integer getNumeroOrden() { return numeroOrden; }
    public void setNumeroOrden(Integer numeroOrden) { this.numeroOrden = numeroOrden; }
}
