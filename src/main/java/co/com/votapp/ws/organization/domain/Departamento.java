package co.com.votapp.ws.organization.domain;

/**
 * Agregado raíz del módulo de organización.
 *
 * <p>Representa un departamento de la institución.
 * No contiene anotaciones de Spring ni de JPA — pertenece exclusivamente al dominio.
 */
public class Departamento {

    private final Integer id;
    private final String codigo;
    private final String nombre;
    private final boolean activo;

    public Departamento(Integer id, String codigo, String nombre, boolean activo) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (codigo == null || codigo.isBlank()) throw new IllegalArgumentException("codigo must not be blank");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        this.id = id;
        this.codigo = codigo;
        this.nombre = nombre;
        this.activo = activo;
    }

    public Integer getId() { return id; }
    public String getCodigo() { return codigo; }
    public String getNombre() { return nombre; }
    public boolean isActivo() { return activo; }
}
