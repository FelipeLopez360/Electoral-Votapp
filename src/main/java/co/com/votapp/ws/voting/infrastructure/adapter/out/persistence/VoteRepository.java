package co.com.votapp.ws.voting.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VoteRepository extends JpaRepository<VoteEntity, UUID> {
}
