package co.com.votapp.ws.voting.application.service;

import co.com.votapp.ws.voting.domain.port.in.CastVoteByTokenIdPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Application service that adds {@code @Transactional} semantics to vote casting.
 *
 * <p>This is a thin wrapper in the application layer. Its ONLY responsibility is to
 * provide a Spring-managed transaction boundary around the domain use case call.
 * It contains NO business logic — all rules live in the domain use case.
 *
 * <h3>Why this exists</h3>
 * <p>Domain use cases ({@link CastVoteByTokenIdUseCaseImpl}) are pure Java with no
 * Spring annotations. {@code @Transactional} requires a Spring proxy. This application
 * service provides the proxy boundary while keeping the domain clean.
 *
 * <h3>Portal flow only</h3>
 * <p>The legacy rawToken-based {@code castVote(CastVoteCommand)} has been removed.
 * The canonical flow uses {@code castVoteByTokenId}, where the token UUID is resolved
 * from the authenticated portal session (rawToken is never sent over the wire).
 */
@Service
public class CastVoteAppService {

    private final CastVoteByTokenIdPort castVoteByTokenIdPort;

    public CastVoteAppService(CastVoteByTokenIdPort castVoteByTokenIdPort) {
        this.castVoteByTokenIdPort = castVoteByTokenIdPort;
    }

    /**
     * Cast a vote using an internal token UUID (portal-flow).
     *
     * <p>Delegates to {@link CastVoteByTokenIdPort#castVote(UUID, UUID)} within a transaction.
     * The portal resolves the token by its UUID (rawToken is unrecoverable from SHA-256 hash).
     *
     * @param tokenId     the internal UUID of the ISSUED voting token
     * @param candidatoId the UUID of the selected candidate
     */
    @Transactional
    public void castVoteByTokenId(UUID tokenId, UUID candidatoId) {
        castVoteByTokenIdPort.castVote(tokenId, candidatoId);
    }
}
