package co.com.votapp.ws.organization.domain.port.out;

import co.com.votapp.ws.organization.domain.Departamento;

import java.util.List;

/**
 * Puerto de salida para acceder al repositorio de departamentos.
 */
public interface DepartamentoRepositoryPort {
    List<Departamento> findAllByActivoTrue();
}
