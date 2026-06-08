package co.com.votapp.ws.audit.domain.port.out;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;

/**
 * Puerto de salida para persistir eventos de auditoría.
 */
public interface AuditoriaRepositoryPort {
    void save(AuditoriaEvento evento);
}
