package co.com.votapp.ws.auth.domain;

/**
 * Aggregado raíz del módulo de autenticación.
 *
 * <p>Representa a un funcionario que puede autenticarse en el sistema.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class Funcionario {

    private final Integer id;
    private final String numeroEmpleado;
    private final String documentoIdentidad;
    private final String email;
    private final boolean puedeVotar;
    private final String estadoLaboral;

    public Funcionario(Integer id,
                       String numeroEmpleado,
                       String documentoIdentidad,
                       String email,
                       boolean puedeVotar,
                       String estadoLaboral) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (numeroEmpleado == null || numeroEmpleado.isBlank())
            throw new IllegalArgumentException("numeroEmpleado must not be blank");
        if (documentoIdentidad == null || documentoIdentidad.isBlank())
            throw new IllegalArgumentException("documentoIdentidad must not be blank");
        this.id = id;
        this.numeroEmpleado = numeroEmpleado;
        this.documentoIdentidad = documentoIdentidad;
        this.email = email;
        this.puedeVotar = puedeVotar;
        this.estadoLaboral = estadoLaboral;
    }

    public Integer getId() { return id; }
    public String getNumeroEmpleado() { return numeroEmpleado; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getEmail() { return email; }
    public boolean isPuedeVotar() { return puedeVotar; }
    public String getEstadoLaboral() { return estadoLaboral; }

    public boolean isActivo() {
        return "ACTIVO".equals(estadoLaboral);
    }
}
