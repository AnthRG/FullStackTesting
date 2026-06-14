package pucmm.freddy.fullstacktesting.dto;

/**
 * Rol de realm que puede asignarse a un usuario.
 *
 * @param name        nombre del rol (ej. {@code VIEW_ROLES}, {@code EDIT_ROLES})
 * @param description descripcion legible (puede ser {@code null})
 */
public record RoleView(String name, String description) {
}
