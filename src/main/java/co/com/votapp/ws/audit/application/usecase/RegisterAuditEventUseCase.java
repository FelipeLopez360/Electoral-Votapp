package co.com.votapp.ws.audit.application.usecase;

import co.com.votapp.ws.audit.application.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.audit.application.port.out.AuditoriaRepositoryPort;
import co.com.votapp.ws.audit.domain.AuditoriaEvento;
import co.com.votapp.ws.common.annotation.UseCase;

/**
 * Caso de uso: registra un evento de auditoría en el sistema.
 */
@UseCase
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
