package co.com.votapp.ws.config;

import co.com.votapp.ws.audit.domain.port.in.RegisterAuditEventPort;
import co.com.votapp.ws.audit.domain.port.out.AuditoriaRepositoryPort;
import co.com.votapp.ws.audit.domain.usecase.RegisterAuditEventUseCase;
import co.com.votapp.ws.auth.domain.port.out.FuncionarioRepositoryPort;
import co.com.votapp.ws.auth.domain.usecase.AuthenticateFuncionarioUseCase;
import co.com.votapp.ws.candidates.domain.port.out.CandidatoRepositoryPort;
import co.com.votapp.ws.candidates.domain.usecase.GetCandidatosUseCase;
import co.com.votapp.ws.electoral.domain.port.in.ActivateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.AddCandidateUseCase;
import co.com.votapp.ws.electoral.domain.port.in.CreateElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.FinalizeElectionUseCase;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.EleccionRepositoryPort;
import co.com.votapp.ws.electoral.domain.usecase.ActivateElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.AddCandidateUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.CreateElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.FinalizeElectionUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.GetBallotUseCaseImpl;
import co.com.votapp.ws.electoral.domain.usecase.GetEleccionUseCase;
import co.com.votapp.ws.organization.domain.port.out.DepartamentoRepositoryPort;
import co.com.votapp.ws.organization.domain.usecase.GetDepartamentosUseCase;
import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.TokenLockPort;
import co.com.votapp.ws.voting.domain.port.out.VoteRepositoryPort;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;
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
    public AuthenticateFuncionarioUseCase authenticateFuncionarioUseCase(FuncionarioRepositoryPort funcionarioRepository) {
        return new AuthenticateFuncionarioUseCase(funcionarioRepository);
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
                                                                VoterEligibilityRepositoryPort eligibilityRepository) {
        return new IssueVotingTokenUseCaseImpl(votingTokenRepository, eligibilityRepository);
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
}
