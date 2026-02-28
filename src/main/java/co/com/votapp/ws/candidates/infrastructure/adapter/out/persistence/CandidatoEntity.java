package co.com.votapp.ws.candidates.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entidad JPA para la tabla {@code test_votaappdb.candidatos}.
 * Solo se usa en la capa de infraestructura.
 */
@Entity
@Table(name = "candidatos", schema = "test_votaappdb")
public class CandidatoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", updatable = false, insertable = false)
    private UUID uuid;

    @Column(name = "eleccion_id", nullable = false)
    private Integer eleccionId;

    @Column(name = "categoria_id")
    private Integer categoriaId;

    @Column(name = "nombres", nullable = false)
    private String nombres;

    @Column(name = "apellidos", nullable = false)
    private String apellidos;

    @Column(name = "es_voto_blanco")
    private Boolean esVotoBlanco;

    @Column(name = "activo")
    private Boolean activo;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public Integer getEleccionId() { return eleccionId; }
    public void setEleccionId(Integer eleccionId) { this.eleccionId = eleccionId; }

    public Integer getCategoriaId() { return categoriaId; }
    public void setCategoriaId(Integer categoriaId) { this.categoriaId = categoriaId; }

    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }

    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }

    public Boolean getEsVotoBlanco() { return esVotoBlanco; }
    public void setEsVotoBlanco(Boolean esVotoBlanco) { this.esVotoBlanco = esVotoBlanco; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
