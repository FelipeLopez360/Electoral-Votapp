package co.com.votapp.ws.electoral.domain.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TDD RED cycle — tests CensoEntry domain record construction and value equality.
 * Written BEFORE the production class exists.
 */
@DisplayName("CensoEntry - Domain record construction and equality")
class CensoEntryTest {

    private static final UUID ELECCION_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID ENTRY_ID    = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final Integer FUNCIONARIO_ID = 1;

    @Test
    @DisplayName("Should hold all fields correctly when constructed")
    void censoEntry_shouldHoldFields_whenConstructed() {
        // Given
        Instant now = Instant.parse("2026-01-01T10:00:00Z");
        Integer agregadoPor = 99;  // admin funcionario_id

        // When
        var entry = new CensoEntry(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, agregadoPor, now);

        // Then
        assertThat(entry.id()).isEqualTo(ENTRY_ID);
        assertThat(entry.eleccionId()).isEqualTo(ELECCION_ID);
        assertThat(entry.funcionarioId()).isEqualTo(FUNCIONARIO_ID);
        assertThat(entry.agregadoPor()).isEqualTo(99);
        assertThat(entry.fechaAgregado()).isEqualTo(now);
    }

    @Test
    @DisplayName("Should be equal when all fields are the same")
    void censoEntry_shouldBeEqual_whenFieldsMatch() {
        // Given
        Instant ts = Instant.parse("2026-01-01T10:00:00Z");

        // When
        var a = new CensoEntry(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, 99, ts);
        var b = new CensoEntry(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, 99, ts);

        // Then
        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    @DisplayName("Should not be equal when funcionarioId differs")
    void censoEntry_shouldNotBeEqual_whenFuncionarioIdDiffers() {
        // Given
        Instant ts = Instant.parse("2026-01-01T10:00:00Z");

        // When
        var a = new CensoEntry(ENTRY_ID, ELECCION_ID, 1, 99, ts);
        var b = new CensoEntry(ENTRY_ID, ELECCION_ID, 2, 99, ts);

        // Then
        assertThat(a).isNotEqualTo(b);
    }

    @Test
    @DisplayName("Should allow null agregadoPor — nullable admin FK in MVP")
    void censoEntry_shouldAllowNullAgregadoPor_forMvp() {
        // Given / When
        var entry = new CensoEntry(ENTRY_ID, ELECCION_ID, FUNCIONARIO_ID, (Integer) null, Instant.now());

        // Then
        assertThat(entry.agregadoPor()).isNull();
    }

    @Test
    @DisplayName("Should allow null id (before persistence)")
    void censoEntry_shouldAllowNullId_beforePersistence() {
        // Given / When
        var entry = new CensoEntry(null, ELECCION_ID, FUNCIONARIO_ID, null, Instant.now());

        // Then
        assertThat(entry.id()).isNull();
    }
}
