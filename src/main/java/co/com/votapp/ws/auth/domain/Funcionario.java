package co.com.votapp.ws.auth.domain;

import java.util.UUID;

/**
 * Aggregado raíz del módulo de autenticación.
 *
 * <p>Representa a un funcionario que puede autenticarse en el sistema.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class Funcionario {

    private final UUID uuid;
    private final String numeroEmpleado;
    private final String documentoIdentidad;
    private final String email;
    private final boolean puedeVotar;
    private final String estadoLaboral;

    public Funcionario(UUID uuid,
                       String numeroEmpleado,
                       String documentoIdentidad,
                       String email,
                       boolean puedeVotar,
                       String estadoLaboral) {
        if (uuid == null) throw new IllegalArgumentException("uuid must not be null");
        if (numeroEmpleado == null || numeroEmpleado.isBlank())
            throw new IllegalArgumentException("numeroEmpleado must not be blank");
        if (documentoIdentidad == null || documentoIdentidad.isBlank())
            throw new IllegalArgumentException("documentoIdentidad must not be blank");
        this.uuid = uuid;
        this.numeroEmpleado = numeroEmpleado;
        this.documentoIdentidad = documentoIdentidad;
        this.email = email;
        this.puedeVotar = puedeVotar;
        this.estadoLaboral = estadoLaboral;
    }

    public UUID getUuid() { return uuid; }
    public String getNumeroEmpleado() { return numeroEmpleado; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getEmail() { return email; }
    public boolean isPuedeVotar() { return puedeVotar; }
    public String getEstadoLaboral() { return estadoLaboral; }

    public boolean isActivo() {
        return "ACTIVO".equals(estadoLaboral);
    }
}
