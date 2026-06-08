package co.com.votapp.ws.organization.domain;

/**
 * Represents a job position (cargo) in the organization.
 *
 * <p>Pure Java — zero Spring or JPA imports. Maps to the {@code cargos} table
 * via the infrastructure adapter.
 */
public class Cargo {

    private final int id;
    private final String codigo;
    private final String nombre;
    private final int nivelJerarquico;
    private final boolean activo;

    public Cargo(int id, String codigo, String nombre, int nivelJerarquico, boolean activo) {
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo must not be blank");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.nivelJerarquico = nivelJerarquico;
        this.activo = activo;
    }

    public int getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public int getNivelJerarquico() { return nivelJerarquico; }
    public boolean isActivo() { return activo; }
}
