package pucmm.freddy.fullstacktesting.web;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import pucmm.freddy.fullstacktesting.dto.RoleView;
import pucmm.freddy.fullstacktesting.dto.UserRolesView;
import pucmm.freddy.fullstacktesting.service.KeycloakAdminClient;

/**
 * Administracion de los roles de los usuarios contra Keycloak.
 *
 * <p><b>SIN PROTEGER por ahora</b> (ver {@code SecurityConfig}, donde
 * {@code /api/admin/**} esta en {@code permitAll}). Antes de produccion estos
 * endpoints deben exigir los permisos del negocio:
 * <ul>
 *   <li>lectura ({@code GET}) → {@code @PreAuthorize("hasRole('VIEW_ROLES')")}</li>
 *   <li>escritura ({@code POST}/{@code DELETE}) → {@code @PreAuthorize("hasRole('EDIT_ROLES')")}</li>
 * </ul>
 *
 * <p>Las firmas usan records (DTOs) y verbos/estados HTTP estandar, de modo que
 * agregar OpenAPI/Swagger (springdoc) despues no requiera reescribir nada: solo
 * anadir las anotaciones {@code @Tag}/{@code @Operation} si se desea.</p>
 */
@RestController
@RequestMapping("/api/admin")
public class UserRolesController {

    private final KeycloakAdminClient keycloak;

    public UserRolesController(KeycloakAdminClient keycloak) {
        this.keycloak = keycloak;
    }

    /**
     * Lista todos los usuarios del realm con sus roles asignados.
     *
     * @return usuarios con sus roles de realm
     */
    @GetMapping("/users")
    public List<UserRolesView> listUsers() {
        return keycloak.listUsers();
    }

    /**
     * Devuelve un usuario concreto y sus roles.
     *
     * @param id id del usuario en Keycloak
     * @return el usuario; {@code 404} si no existe
     */
    @GetMapping("/users/{id}")
    public UserRolesView getUser(@PathVariable String id) {
        return keycloak.getUser(id);
    }

    /**
     * Lista los roles de realm que se pueden asignar (excluye los internos de Keycloak).
     *
     * @return roles asignables
     */
    @GetMapping("/roles")
    public List<RoleView> listRoles() {
        return keycloak.assignableRoles();
    }

    /**
     * Asigna un rol existente a un usuario.
     *
     * @param id   id del usuario
     * @param role nombre del rol (ej. {@code VIEW_ROLES}); {@code 404} si el rol no existe
     */
    @PostMapping("/users/{id}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void assignRole(@PathVariable String id, @PathVariable String role) {
        keycloak.assignRole(id, role);
    }

    /**
     * Quita un rol a un usuario.
     *
     * @param id   id del usuario
     * @param role nombre del rol a remover; {@code 404} si el rol no existe
     */
    @DeleteMapping("/users/{id}/roles/{role}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeRole(@PathVariable String id, @PathVariable String role) {
        keycloak.removeRole(id, role);
    }
}
