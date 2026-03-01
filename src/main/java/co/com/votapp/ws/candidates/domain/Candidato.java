package co.com.votapp.ws.candidates.domain;

import java.util.UUID;

/**
 * Agregado raíz del módulo de candidatos.
 *
 * <p>Representa un candidato en una elección institucional.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class Candidato {

    private final UUID uuid;
    private final Integer eleccionId;
    private final Integer categoriaId;
    private final String nombres;
    private final String apellidos;
    private final boolean esVotoBlanco;
    private final boolean activo;

    public Candidato(UUID uuid,
                     Integer eleccionId,
                     Integer categoriaId,
                     String nombres,
                     String apellidos,
                     boolean esVotoBlanco,
                     boolean activo) {
        if (uuid == null) throw new IllegalArgumentException("uuid must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (categoriaId == null) throw new IllegalArgumentException("categoriaId must not be null");
        if (nombres == null || nombres.isBlank()) throw new IllegalArgumentException("nombres must not be blank");
        if (apellidos == null || apellidos.isBlank()) throw new IllegalArgumentException("apellidos must not be blank");
        this.uuid = uuid;
        this.eleccionId = eleccionId;
        this.categoriaId = categoriaId;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.esVotoBlanco = esVotoBlanco;
        this.activo = activo;
    }

    public UUID getUuid() { return uuid; }
    public Integer getEleccionId() { return eleccionId; }
    public Integer getCategoriaId() { return categoriaId; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public boolean isEsVotoBlanco() { return esVotoBlanco; }
    public boolean isActivo() { return activo; }

    public String getNombreCompleto() {
        return nombres + " " + apellidos;
    }
}
