package co.com.votapp.ws.electoral.application.dto;

/**
 * Response for a bulk census add operation.
 *
 * @param added   number of funcionarios newly added to the census
 * @param skipped number of funcionarios already in census — skipped
 * @param total   total number of funcionarios matching the filter criteria
 */
public record BulkAddCensoResponse(int added, int skipped, int total) {}
