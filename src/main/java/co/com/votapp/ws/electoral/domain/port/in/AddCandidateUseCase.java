package co.com.votapp.ws.electoral.domain.port.in;

import co.com.votapp.ws.electoral.application.command.AddCandidateCommand;
import co.com.votapp.ws.electoral.domain.Candidate;

/**
 * Input port: add a candidate to an election that is not FINALIZADA or CANCELADA.
 */
public interface AddCandidateUseCase {
    Candidate addCandidate(AddCandidateCommand command);
}
