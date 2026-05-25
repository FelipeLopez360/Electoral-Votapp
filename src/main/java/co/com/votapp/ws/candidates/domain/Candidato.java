package co.com.votapp.ws.candidates.domain;

import java.util.UUID;

/**
 * Domain aggregate for a candidate in an election (MVP schema).
 * No Spring or JPA annotations — pure Java.
 */
public class Candidato {

    private final UUID id;
    private final UUID eleccionId;
    private final String nombre;
    private final boolean esVotoEnBlanco;
    private final int numeroOrden;

    public Candidato(UUID id,
                     UUID eleccionId,
                     String nombre,
                     boolean esVotoEnBlanco,
                     int numeroOrden) {
        if (id == null) throw new IllegalArgumentException("id must not be null");
        if (eleccionId == null) throw new IllegalArgumentException("eleccionId must not be null");
        if (nombre == null || nombre.isBlank()) throw new IllegalArgumentException("nombre must not be blank");
        this.id = id;
        this.eleccionId = eleccionId;
        this.nombre = nombre;
        this.esVotoEnBlanco = esVotoEnBlanco;
        this.numeroOrden = numeroOrden;
    }

    public UUID getId() { return id; }
    public UUID getEleccionId() { return eleccionId; }
    public String getNombre() { return nombre; }
    public boolean isEsVotoEnBlanco() { return esVotoEnBlanco; }
    public int getNumeroOrden() { return numeroOrden; }
}
