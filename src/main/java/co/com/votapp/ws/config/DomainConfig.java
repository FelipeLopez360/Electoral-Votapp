package co.com.votapp.ws.config;

import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;
import co.com.votapp.ws.audit.domain.usecase.RegisterAuditEventUseCase;
import co.com.votapp.ws.auth.domain.port.in.ChangePasswordUseCase;
import co.com.votapp.ws.auth.domain.port.in.CreateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.LogoutUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.port.in.UpdateProfileUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.port.out.PasswordEncoderPort;
import co.com.votapp.ws.auth.domain.port.out.PortalSessionPort;
import co.com.votapp.ws.auth.domain.usecase.AuthenticateFuncionarioUseCase;
import co.com.votapp.ws.auth.domain.usecase.ChangePasswordUseCaseImpl;
import co.com.votapp.ws.auth.domain.usecase.CreateFuncionarioUseCaseImpl;
import co.com.votapp.ws.auth.domain.usecase.LogoutUseCaseImpl;
import co.com.votapp.ws.auth.domain.usecase.UpdateFuncionarioUseCaseImpl;
import co.com.votapp.ws.auth.domain.usecase.UpdateProfileUseCaseImpl;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;
import co.com.votapp.ws.candidates.domain.usecase.GetCandidatosUseCase;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.in.ManageCensoUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.CensoRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.EleccionRepositoryPort;
import co.com.votapp.ws.electoral.domain.usecase.ActivateElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.AddCandidateUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.CreateElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.FinalizeElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.GetBallotUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.GetEleccionUseCase;
import co.com.votapp.ws.electoral.domain.usecase.ManageCensoUseCaseImpl;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import co.com.votapp.ws.organization.domain.usecase.GetDepartamentosUseCase;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
import co.com.votapp.ws.voting.domain.usecase.CastVoteByTokenIdUseCaseImpl;
import co.com.votapp.ws.voting.domain.usecase.CastVoteUseCaseImpl;
import co.com.votapp.ws.voting.domain.usecase.IssueVotingTokenUseCaseImpl;
import co.com.votapp.ws.votereligibility.domain.port.out.VoterEligibilityRepositoryPort;
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

    // ─── Legacy use cases (pre-MVP-base) ─────────────────────────────────────

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
    public AuthenticateFuncionarioUseCase authenticateFuncionarioUseCase(FuncionarioRepositoryPort funcionarioRepository,
                                                                          PasswordEncoderPort passwordEncoder) {
        return new AuthenticateFuncionarioUseCase(funcionarioRepository, passwordEncoder);
    }

    // ─── gestion-funcionarios: Funcionario CRUD use cases ────────────────────

    @Bean
    public ChangePasswordUseCase changePasswordUseCase(FuncionarioRepositoryPort funcionarioRepository,
                                                        PasswordEncoderPort passwordEncoder) {
        return new ChangePasswordUseCaseImpl(funcionarioRepository, passwordEncoder);
    }

    @Bean
    public UpdateProfileUseCase updateProfileUseCase(FuncionarioRepositoryPort funcionarioRepository) {
        return new UpdateProfileUseCaseImpl(funcionarioRepository);
    }

    @Bean
    public LogoutUseCase logoutUseCase(PortalSessionPort sessionPort) {
        return new LogoutUseCaseImpl(sessionPort);
    }

    @Bean
    public CreateFuncionarioUseCase createFuncionarioUseCase(FuncionarioRepositoryPort funcionarioRepository,
                                                              PasswordEncoderPort passwordEncoder) {
        return new CreateFuncionarioUseCaseImpl(funcionarioRepository, passwordEncoder);
    }

    @Bean
    public UpdateFuncionarioUseCase updateFuncionarioUseCase(FuncionarioRepositoryPort funcionarioRepository) {
        return new UpdateFuncionarioUseCaseImpl(funcionarioRepository);
    }

    // ─── censo-electoral: ManageCenso use case ───────────────────────────────

    @Bean
    public ManageCensoUseCase manageCensoUseCase(ElectionRepositoryPort electionRepository,
                                                   FuncionarioRepositoryPort funcionarioRepository,
                                                   CensoRepositoryPort censoRepository) {
        return new ManageCensoUseCaseImpl(electionRepository, funcionarioRepository, censoRepository);
    }

    // ─── PR 2: Electoral use cases ────────────────────────────────────────────

    @Bean
    public CreateElectionUseCase createElectionUseCase(ElectionRepositoryPort electionRepository) {
        return new CreateElectionUseCaseImpl(electionRepository);
    }

    @Bean
    public AddCandidateUseCase addCandidateUseCase(ElectionRepositoryPort electionRepository,
                                                    CandidateRepositoryPort candidateRepository) {
        return new AddCandidateUseCaseImpl(electionRepository, candidateRepository);
    }

    @Bean
    public ActivateElectionUseCase activateElectionUseCase(ElectionRepositoryPort electionRepository,
                                                            CandidateRepositoryPort candidateRepository) {
        return new ActivateElectionUseCaseImpl(electionRepository, candidateRepository);
    }

    @Bean
    public FinalizeElectionUseCase finalizeElectionUseCase(ElectionRepositoryPort electionRepository) {
        return new FinalizeElectionUseCaseImpl(electionRepository);
    }

    @Bean
    public GetBallotUseCase getBallotUseCase(VotingTokenRepository votingTokenRepository,
                                              ElectionRepositoryPort electionRepository,
                                              CandidateRepositoryPort candidateRepository) {
        return new GetBallotUseCaseImpl(votingTokenRepository, electionRepository, candidateRepository);
    }

    // ─── PR 2: Voting use cases ───────────────────────────────────────────────

    @Bean
    public IssueVotingTokenUseCaseImpl issueVotingTokenUseCase(VotingTokenRepository votingTokenRepository,
                                                                VoterEligibilityRepositoryPort eligibilityRepository,
                                                                ParticipacionRepositoryPort participacionRepository) {
        return new IssueVotingTokenUseCaseImpl(votingTokenRepository, eligibilityRepository, participacionRepository);
    }

    @Bean
    public CastVoteUseCaseImpl castVoteUseCase(VotingTokenRepository votingTokenRepository,
                                               TokenLockPort tokenLockPort,
                                               ElectionRepositoryPort electionRepository,
                                               CandidateRepositoryPort candidateRepository,
                                               VoteRepositoryPort voteRepository,
                                               ParticipacionRepositoryPort participacionRepository,
                                               RegisterAuditEventPort auditPort) {
        return new CastVoteUseCaseImpl(
                votingTokenRepository,
                tokenLockPort,
                electionRepository,
                candidateRepository,
                voteRepository,
                participacionRepository,
                auditPort
        );
    }

    @Bean
    public CastVoteByTokenIdUseCaseImpl castVoteByTokenIdUseCase(VotingTokenRepository votingTokenRepository,
                                                                   TokenLockPort tokenLockPort,
                                                                   ElectionRepositoryPort electionRepository,
                                                                   CandidateRepositoryPort candidateRepository,
                                                                   VoteRepositoryPort voteRepository,
                                                                   ParticipacionRepositoryPort participacionRepository,
                                                                   RegisterAuditEventPort auditPort) {
        return new CastVoteByTokenIdUseCaseImpl(
                votingTokenRepository,
                tokenLockPort,
                electionRepository,
                candidateRepository,
                voteRepository,
                participacionRepository,
                auditPort
        );
    }
}
