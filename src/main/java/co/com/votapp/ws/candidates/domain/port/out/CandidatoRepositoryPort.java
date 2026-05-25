package co.com.votapp.ws.candidates.domain.port.out;

import co.com.votapp.ws.candidates.domain.Candidato;

import java.util.List;

/**
 * Puerto de salida para acceder al repositorio de candidatos.
 */
public interface CandidatoRepositoryPort {
    List<Candidato> findByEleccionIdAndActivoTrue(Integer eleccionId);
}
