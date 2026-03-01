package co.com.votapp.ws.audit.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.time.Instant;

/**
 * Entidad JPA para la tabla {@code test_votaappdb.auditoria_sistema}.
 * Solo se usa en la capa de infraestructura.
 */
@Entity
@Table(name = "auditoria_sistema", schema = "test_votaappdb")
public class AuditoriaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "tabla_afectada")
    private String tablaAfectada;

    @Column(name = "registro_id")
    private Integer registroId;

    @Column(name = "accion")
    private String accion;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(name = "usuario_tipo")
    private String usuarioTipo;

    @Column(name = "timestamp_accion")
    private Instant timestampAccion;

    @Column(name = "descripcion")
    private String descripcion;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getTablaAfectada() { return tablaAfectada; }
    public void setTablaAfectada(String tablaAfectada) { this.tablaAfectada = tablaAfectada; }

    public Integer getRegistroId() { return registroId; }
    public void setRegistroId(Integer registroId) { this.registroId = registroId; }

    public String getAccion() { return accion; }
    public void setAccion(String accion) { this.accion = accion; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioTipo() { return usuarioTipo; }
    public void setUsuarioTipo(String usuarioTipo) { this.usuarioTipo = usuarioTipo; }

    public Instant getTimestampAccion() { return timestampAccion; }
    public void setTimestampAccion(Instant timestampAccion) { this.timestampAccion = timestampAccion; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }
}
