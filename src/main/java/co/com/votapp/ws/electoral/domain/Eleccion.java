package co.com.votapp.ws.electoral.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Agregado raíz del módulo electoral.
 *
 * <p>Representa una elección institucional.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class Eleccion {

    private final UUID uuid;
    private final String codigo;
    private final String nombre;
    private final String estado;
    private final Instant fechaInicio;
    private final Instant fechaFin;

    public Eleccion(UUID uuid,
                    String codigo,
                    String nombre,
                    String estado,
                    Instant fechaInicio,
                    Instant fechaFin) {
        if (uuid == null) throw new IllegalArgumentException("uuid must not be null");
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo must not be blank");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        if (estado == null) throw new IllegalArgumentException("estado must not be null");
        if (fechaInicio == null) throw new IllegalArgumentException("fechaInicio must not be null");
        if (fechaFin == null) throw new IllegalArgumentException("fechaFin must not be null");
        if (!fechaFin.isAfter(fechaInicio))
            throw new IllegalArgumentException("fechaFin must be after fechaInicio");
        this.uuid = uuid;
        this.codigo = codigo;
        this.nombre = nombre;
        this.estado = estado;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public UUID getUuid() { return uuid; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public String getEstado() { return estado; }
    public Instant getFechaInicio() { return fechaInicio; }
    public Instant getFechaFin() { return fechaFin; }

    public boolean isActiva() {
        Instant now = Instant.now();
        return "ACTIVA".equals(estado) && !now.isBefore(fechaInicio) && !now.isAfter(fechaFin);
    }
}
