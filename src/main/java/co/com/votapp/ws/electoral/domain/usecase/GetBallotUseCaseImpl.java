package co.com.votapp.ws.electoral.domain.usecase;

import co.com.votapp.ws.common.exception.DomainException;
import co.com.votapp.ws.electoral.domain.Ballot;
import co.com.votapp.ws.electoral.domain.Candidate;
import co.com.votapp.ws.electoral.domain.CandidateOption;
import co.com.votapp.ws.electoral.domain.Election;
import co.com.votapp.ws.electoral.domain.ElectionStatus;
import co.com.votapp.ws.electoral.domain.port.in.GetBallotUseCase;
import co.com.votapp.ws.electoral.domain.port.out.CandidateRepositoryPort;
import co.com.votapp.ws.electoral.domain.port.out.ElectionRepositoryPort;
import co.com.votapp.ws.voting.domain.VotingToken;
import co.com.votapp.ws.voting.domain.port.out.VotingTokenRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

/**
 * Use case implementation: retrieve the ballot for a given raw voting token.
 *
 * <p>Hashes rawToken with SHA-256, looks up the ISSUED token, validates
 * the election is ACTIVA, then returns the ballot with ordered candidates.
 * No Spring annotations — wired manually via DomainConfig.
 */
public class GetBallotUseCaseImpl implements GetBallotUseCase {

    private final VotingTokenRepository votingTokenRepository;
    private final ElectionRepositoryPort electionRepository;
    private final CandidateRepositoryPort candidateRepository;

    public GetBallotUseCaseImpl(VotingTokenRepository votingTokenRepository,
                                ElectionRepositoryPort electionRepository,
                                CandidateRepositoryPort candidateRepository) {
        this.votingTokenRepository = votingTokenRepository;
        this.electionRepository = electionRepository;
        this.candidateRepository = candidateRepository;
    }

    @Override
    public Ballot getBallot(String rawToken) {
        String tokenHash = sha256Hex(rawToken);

        VotingToken token = votingTokenRepository.findIssuedByHash(tokenHash)
                .orElseThrow(() -> new DomainException("Token is invalid or has already been used"));

        Election election = electionRepository.findById(token.eleccionId())
                .orElseThrow(() -> new DomainException("Election not found for token"));

        if (election.status() != ElectionStatus.ACTIVA) {
            throw new DomainException(
                    "Election is not ACTIVA. Current status: " + election.status());
        }

        List<Candidate> candidates = candidateRepository.findByEleccionIdOrderByNumeroOrden(token.eleccionId());

        List<CandidateOption> options = candidates.stream()
                .map(c -> new CandidateOption(c.id(), c.nombre(), c.esVotoEnBlanco()))
                .toList();

        return new Ballot(election.id(), election.nombre(), options);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
