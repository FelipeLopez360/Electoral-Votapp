package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import co.com.votapp.ws.voting.domain.port.out.ParticipacionRepositoryPort;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * Persistence adapter for voter participation recording.
 *
 * <p>Implements {@link ParticipacionRepositoryPort}. Writes a row to
 * {@code participacion_electoral} recording that a funcionario voted.
 * Must be called inside an existing DB transaction (part of the atomic CastVote flow).
 */
@Component
public class ParticipacionRepositoryAdapter implements ParticipacionRepositoryPort {

    private final ParticipacionJpaRepository jpaRepository;

    public ParticipacionRepositoryAdapter(ParticipacionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public void markParticipation(UUID eleccionId, Integer funcionarioId, Instant votedAt) {
        ParticipacionEntity entity = new ParticipacionEntity();
        // id is null → DB generates UUID via @GeneratedValue(UUID)
        entity.setEleccionId(eleccionId);
        entity.setFuncionarioId(funcionarioId);
        entity.setVotedAt(votedAt);
        jpaRepository.save(entity);
    }
}
