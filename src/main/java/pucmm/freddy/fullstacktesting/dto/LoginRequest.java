package pucmm.freddy.fullstacktesting.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Credenciales que envia el frontend al endpoint de login.
 *
 * @param username nombre de usuario
 * @param password contrasena
 */
public record LoginRequest(
        @NotBlank String username,
        @NotBlank String password) {
}
