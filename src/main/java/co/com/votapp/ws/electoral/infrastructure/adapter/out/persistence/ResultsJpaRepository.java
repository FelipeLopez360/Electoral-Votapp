package co.com.votapp.ws.electoral.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/**
 * Spring Data JPA repository for read-only election results aggregation.
 *
 * <p>Runs a single {@code GROUP BY candidato_id} query over the {@code votos} table
 * joined with {@code candidatos} to retrieve per-candidate vote counts and flags.
 * Separate participation and census count queries feed the aggregate totals.
 *
 * <p>Backed by {@link EleccionEntity} as a proxy entity type for JPA bootstrapping;
 * the actual queries target {@code votos}, {@code candidatos}, {@code participacion_electoral},
 * and {@code censo_electoral} via native SQL.
 */
public interface ResultsJpaRepository extends JpaRepository<EleccionEntity, UUID> {

    /**
     * Aggregate vote counts per candidate for the given election.
     *
     * <p>Each row in the result contains:
     * <ol>
     *   <li>{@code UUID} — candidateId</li>
     *   <li>{@code String} — nombre</li>
     *   <li>{@code Long} — vote count</li>
     *   <li>{@code Boolean} — esVotoEnBlanco</li>
     *   <li>{@code Boolean} — esVotoNulo</li>
     * </ol>
     *
     * @param eleccionId the election's UUID
     * @return list of Object[] rows, one per candidate
     */
    @Query(value = """
            SELECT c.id,
                   c.nombre,
                   COUNT(v.id) AS votes,
                   c.es_voto_en_blanco,
                   c.es_voto_nulo
            FROM candidatos c
            LEFT JOIN votos v ON v.candidato_id = c.id
            WHERE c.eleccion_id = :eleccionId
            GROUP BY c.id, c.nombre, c.es_voto_en_blanco, c.es_voto_nulo
            ORDER BY votes DESC
            """,
            nativeQuery = true)
    List<Object[]> findVoteCountsByElection(@Param("eleccionId") UUID eleccionId);

    /**
     * Count the number of voters who actually cast a vote for this election.
     *
     * @param eleccionId the election's UUID
     * @return total participation count
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM participacion_electoral
            WHERE eleccion_id = :eleccionId
            """,
            nativeQuery = true)
    long countParticipation(@Param("eleccionId") UUID eleccionId);

    /**
     * Count the total number of eligible voters in the census for this election.
     *
     * @param eleccionId the election's UUID
     * @return total eligible voter count
     */
    @Query(value = """
            SELECT COUNT(*)
            FROM censo_electoral
            WHERE eleccion_id = :eleccionId
            """,
            nativeQuery = true)
    long countEligible(@Param("eleccionId") UUID eleccionId);
}
