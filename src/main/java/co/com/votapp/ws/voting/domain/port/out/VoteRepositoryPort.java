package co.com.votapp.ws.voting.domain.port.out;

import co.com.votapp.ws.voting.domain.Vote;

/**
 * Output port for anonymous vote persistence.
 *
 * <p>The Vote domain object carries NO funcionario_id — anonymity is guaranteed by design.
 */
public interface VoteRepositoryPort {

    /**
     * Persist an anonymous vote within an existing DB transaction.
     *
     * @param vote the anonymous vote to save
     */
    void save(Vote vote);
}
