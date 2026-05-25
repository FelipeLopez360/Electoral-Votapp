package co.com.votapp.ws.audit.domain.port.in;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;

/**
 * Puerto de entrada para registrar eventos de auditoría.
 */
public interface RegisterAuditEventPort {
    void register(AuditoriaEvento evento);
}
