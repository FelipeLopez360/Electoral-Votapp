package co.com.votapp.ws.organization.application.dto;

/**
 * Response DTO for a cargo lookup entry.
 */
public record CargoResponse(int id, String codigo, String nombre, int nivelJerarquico) {}
