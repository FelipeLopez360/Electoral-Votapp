package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.application.command.CreateElectionCommand;
import co.com.votapp.ws.electoral.domain.Election;

/**
 * Input port: create a new election in PROGRAMADA state.
 */
public interface CreateElectionUseCase {
    Election create(CreateElectionCommand command);
}
