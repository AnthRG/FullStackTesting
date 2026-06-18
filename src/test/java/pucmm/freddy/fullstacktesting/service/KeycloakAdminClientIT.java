package pucmm.freddy.fullstacktesting.service;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.server.ResponseStatusException;
import pucmm.freddy.fullstacktesting.AbstractIntegrationTest;
import pucmm.freddy.fullstacktesting.dto.RoleView;
import pucmm.freddy.fullstacktesting.dto.UserRolesView;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class KeycloakAdminClientIT extends AbstractIntegrationTest {

    @Autowired
    private KeycloakAdminClient keycloak;

    @Test
    void listUsers_devuelveLosUsuariosDelRealm() {
        List<UserRolesView> users = keycloak.listUsers();

        assertThat(users).extracting(UserRolesView::username)
                .contains("admin", "user1", "user2");
    }

    @Test
    void getUser_devuelveSusRolesDeRealm() {
        UserRolesView admin = keycloak.getUser(userId("admin"));

        assertThat(admin.username()).isEqualTo("admin");
        assertThat(admin.realmRoles())
                .contains("VIEW_ROLES", "EDIT_ROLES", "product:view", "product:manage");
    }

    @Test
    void assignableRoles_excluyeInternosYDevuelveLosDeNegocio() {
        List<String> names = keycloak.assignableRoles().stream().map(RoleView::name).toList();

        assertThat(names)
                .contains("VIEW_ROLES", "EDIT_ROLES", "product:view", "product:manage")
                .doesNotContain("offline_access", "uma_authorization");
    }

    @Test
    void assignYRemoveRole_seReflejaEnElUsuario() {
        String user2 = userId("user2");
        if (keycloak.getUser(user2).realmRoles().contains("EDIT_ROLES")) {
            keycloak.removeRole(user2, "EDIT_ROLES");
        }

        keycloak.assignRole(user2, "EDIT_ROLES");
        assertThat(keycloak.getUser(user2).realmRoles()).contains("EDIT_ROLES");

        keycloak.removeRole(user2, "EDIT_ROLES");
        assertThat(keycloak.getUser(user2).realmRoles()).doesNotContain("EDIT_ROLES");
    }

    @Test
    void assignRole_conRolInexistente_lanza404() {
        String user2 = userId("user2");

        assertThatThrownBy(() -> keycloak.assignRole(user2, "ROL_QUE_NO_EXISTE"))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode().value())
                .isEqualTo(404);
    }

    private String userId(String username) {
        return keycloak.listUsers().stream()
                .filter(u -> username.equals(u.username()))
                .map(UserRolesView::id)
                .findFirst()
                .orElseThrow();
    }
}
