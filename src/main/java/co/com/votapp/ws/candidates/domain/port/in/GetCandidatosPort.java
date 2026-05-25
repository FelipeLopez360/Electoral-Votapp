package co.com.votapp.ws.candidates.domain.port.in;

import co.com.votapp.ws.candidates.domain.Candidato;

import java.util.List;

/**
 * Puerto de entrada para listar candidatos de una elección.
 */
public interface GetCandidatosPort {
    List<Candidato> findByEleccionId(Integer eleccionId);
}
