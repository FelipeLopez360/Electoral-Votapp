package co.com.votapp.ws.audit.domain;

import java.time.Instant;

/**
 * Agregado raíz del módulo de auditoría.
 *
 * <p>Representa un evento de auditoría registrado en el sistema.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class AuditoriaEvento {

    private final String tablaAfectada;
    private final Integer registroId;
    private final String accion;
    private final Integer usuarioId;
    private final String usuarioTipo;
    private final Instant timestampAccion;
    private final String descripcion;

    public AuditoriaEvento(String tablaAfectada,
                           Integer registroId,
                           String accion,
                           Integer usuarioId,
                           String usuarioTipo,
                           Instant timestampAccion,
                           String descripcion) {
        if (tablaAfectada == null || tablaAfectada.isBlank())
            throw new IllegalArgumentException("tablaAfectada must not be blank");
        if (accion == null || accion.isBlank())
            throw new IllegalArgumentException("accion must not be blank");
        if (timestampAccion == null)
            throw new IllegalArgumentException("timestampAccion must not be null");
        this.tablaAfectada = tablaAfectada;
        this.registroId = registroId;
        this.accion = accion;
        this.usuarioId = usuarioId;
        this.usuarioTipo = usuarioTipo;
        this.timestampAccion = timestampAccion;
        this.descripcion = descripcion;
    }

    public String getTablaAfectada() { return tablaAfectada; }
    public Integer getRegistroId() { return registroId; }
    public String getAccion() { return accion; }
    public Integer getUsuarioId() { return usuarioId; }
    public String getUsuarioTipo() { return usuarioTipo; }
    public Instant getTimestampAccion() { return timestampAccion; }
    public String getDescripcion() { return descripcion; }
}
