package co.com.votapp.ws.auth.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

import java.util.UUID;

/**
 * Entidad JPA para la tabla {@code test_votaappdb.funcionarios}.
 * Solo se usa en la capa de infraestructura.
 */
@Entity
@Table(name = "funcionarios", schema = "test_votaappdb")
public class FuncionarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "uuid", updatable = false, insertable = false)
    private UUID uuid;

    @Column(name = "numero_empleado", nullable = false, unique = true)
    private String numeroEmpleado;

    @Column(name = "documento_identidad", nullable = false, unique = true)
    private String documentoIdentidad;

    @Column(name = "email")
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(name = "puede_votar")
    private Boolean puedeVotar;

    @Column(name = "estado_laboral")
    private String estadoLaboral;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getNumeroEmpleado() { return numeroEmpleado; }
    public void setNumeroEmpleado(String numeroEmpleado) { this.numeroEmpleado = numeroEmpleado; }

    public String getDocumentoIdentidad() { return documentoIdentidad; }
    public void setDocumentoIdentidad(String documentoIdentidad) { this.documentoIdentidad = documentoIdentidad; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Boolean getPuedeVotar() { return puedeVotar; }
    public void setPuedeVotar(Boolean puedeVotar) { this.puedeVotar = puedeVotar; }

    public String getEstadoLaboral() { return estadoLaboral; }
    public void setEstadoLaboral(String estadoLaboral) { this.estadoLaboral = estadoLaboral; }
}
