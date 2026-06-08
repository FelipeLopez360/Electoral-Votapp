package co.com.votapp.ws.audit.domain.usecase;

import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;

/**
 * Caso de uso: registra un evento de auditoría en el sistema.
 */
public class RegisterAuditEventUseCase implements RegisterAuditEventPort {

    private final AuditoriaRepositoryPort repositoryPort;

    public RegisterAuditEventUseCase(AuditoriaRepositoryPort repositoryPort) {
        this.repositoryPort = repositoryPort;
    }

    @Override
    public void register(AuditoriaEvento evento) {
        repositoryPort.save(evento);
    }
}
