package co.com.votapp.ws.organization.infrastructure.adapter.out.persistence;

import jakarta.persistence.*;

/**
 * JPA entity for the {@code cargos} table.
 * Lives in the infrastructure layer only — never imported by domain.
 */
@Entity
@Table(name = "cargos")
public class CargoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "codigo", nullable = false, unique = true)
    private String codigo;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "nivel_jerarquico")
    private Integer nivelJerarquico;

    @Column(name = "activo")
    private Boolean activo;

    // ─── Getters & Setters ───────────────────────────────────────────────────

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public Integer getNivelJerarquico() { return nivelJerarquico; }
    public void setNivelJerarquico(Integer nivelJerarquico) { this.nivelJerarquico = nivelJerarquico; }

    public Boolean getActivo() { return activo; }
    public void setActivo(Boolean activo) { this.activo = activo; }
}
