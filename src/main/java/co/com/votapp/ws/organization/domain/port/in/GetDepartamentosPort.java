package co.com.votapp.ws.organization.domain.port.in;

import co.com.votapp.ws.organization.domain.Departamento;

import java.util.List;

/**
 * Puerto de entrada para consultar departamentos de la organización.
 */
public interface GetDepartamentosPort {
    List<Departamento> findAllActivos();
}
