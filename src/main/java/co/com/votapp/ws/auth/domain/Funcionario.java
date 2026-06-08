package co.com.votapp.ws.auth.domain;

import java.time.LocalDate;

/**
 * Aggregado raíz del módulo de autenticación.
 *
 * <p>Representa a un funcionario que puede autenticarse en el sistema.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 *
 * <p>El campo {@code id} puede ser {@code null} antes de la persistencia (create flow).
 */
public class Funcionario {

    private final Integer id;
    private final String numeroEmpleado;
    private final String documentoIdentidad;
    private final String nombres;
    private final String apellidos;
    private final String tipoDocumento;
    private final String email;
    private final String telefono;
    private final Integer departamentoId;
    private final Integer cargoId;
    private final LocalDate fechaIngreso;
    private final boolean puedeVotar;
    private final String estadoLaboral;
    private final boolean debeCambiarPassword;

    public Funcionario(Integer id,
                       String numeroEmpleado,
                       String documentoIdentidad,
                       String nombres,
                       String apellidos,
                       String tipoDocumento,
                       String email,
                       String telefono,
                       Integer departamentoId,
                       Integer cargoId,
                       LocalDate fechaIngreso,
                       boolean puedeVotar,
                       String estadoLaboral,
                       boolean debeCambiarPassword) {
        if (numeroEmpleado != null && numeroEmpleado.isBlank())
            throw new IllegalArgumentException("numeroEmpleado must not be blank");
        if (documentoIdentidad == null || documentoIdentidad.isBlank())
            throw new IllegalArgumentException("documentoIdentidad must not be blank");
        // id may be null before persistence (create flow)
        this.id = id;
        this.numeroEmpleado = numeroEmpleado;
        this.documentoIdentidad = documentoIdentidad;
        this.nombres = nombres;
        this.apellidos = apellidos;
        this.tipoDocumento = tipoDocumento;
        this.email = email;
        this.telefono = telefono;
        this.departamentoId = departamentoId;
        this.cargoId = cargoId;
        this.fechaIngreso = fechaIngreso;
        this.puedeVotar = puedeVotar;
        this.estadoLaboral = estadoLaboral;
        this.debeCambiarPassword = debeCambiarPassword;
    }

    public Integer getId() { return id; }
    public String getNumeroEmpleado() { return numeroEmpleado; }
    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public String getNombres() { return nombres; }
    public String getApellidos() { return apellidos; }
    public String getTipoDocumento() { return tipoDocumento; }
    public String getEmail() { return email; }
    public String getTelefono() { return telefono; }
    public Integer getDepartamentoId() { return departamentoId; }
    public Integer getCargoId() { return cargoId; }
    public LocalDate getFechaIngreso() { return fechaIngreso; }
    public boolean isPuedeVotar() { return puedeVotar; }
    public String getEstadoLaboral() { return estadoLaboral; }
    public boolean isDebeCambiarPassword() { return debeCambiarPassword; }

    public boolean isActivo() {
        return "ACTIVO".equals(estadoLaboral);
    }
}
