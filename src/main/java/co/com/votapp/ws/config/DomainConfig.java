package co.com.votapp.ws.config;

import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;
import co.com.votapp.ws.audit.domain.usecase.RegisterAuditEventUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.usecase.AuthenticateFuncionarioUseCase;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;
import co.com.votapp.ws.candidates.domain.usecase.GetCandidatosUseCase;
import co.com.votapp.ws.electoral.domain.port.out.EleccionRepositoryPort;
import co.com.votapp.ws.electoral.domain.usecase.GetEleccionUseCase;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import co.com.votapp.ws.organization.domain.usecase.GetDepartamentosUseCase;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.usecase.CastVoteUseCaseImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Manual wiring of domain use cases.
 *
 * <p>Use cases live in domain/usecase/ with ZERO Spring annotations.
 * This configuration class is the ONLY place where they are wired into Spring beans.
 * No @ComponentScan reaches domain/ packages.
 */
@Configuration
public class DomainConfig {

    @Bean
    public GetEleccionUseCase getEleccionUseCase(EleccionRepositoryPort eleccionRepository) {
        return new GetEleccionUseCase(eleccionRepository);
    }

    @Bean
    public RegisterAuditEventUseCase registerAuditEventUseCase(AuditoriaRepositoryPort auditoriaRepository) {
        return new RegisterAuditEventUseCase(auditoriaRepository);
    }

    @Bean
    public GetDepartamentosUseCase getDepartamentosUseCase(DepartamentoRepositoryPort departamentoRepository) {
        return new GetDepartamentosUseCase(departamentoRepository);
    }

    @Bean
    public GetCandidatosUseCase getCandidatosUseCase(CandidatoRepositoryPort candidatoRepository) {
        return new GetCandidatosUseCase(candidatoRepository);
    }

    @Bean
    public CastVoteUseCaseImpl castVoteUseCase(TokenLockPort tokenLockPort) {
        return new CastVoteUseCaseImpl(tokenLockPort);
    }

    @Bean
    public AuthenticateFuncionarioUseCase authenticateFuncionarioUseCase(FuncionarioRepositoryPort funcionarioRepository) {
        return new AuthenticateFuncionarioUseCase(funcionarioRepository);
    }
}
