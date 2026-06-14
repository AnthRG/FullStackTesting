package pucmm.freddy.fullstacktesting.dto;

/**
 * Token devuelto al frontend tras un login exitoso. Es el access token (JWT)
 * emitido por Keycloak que luego se manda como {@code Authorization: Bearer ...}
 * a los endpoints protegidos.
 *
 * @param accessToken  JWT de acceso
 * @param tokenType    tipo de token (normalmente {@code Bearer})
 * @param expiresIn    segundos de vida del access token
 * @param refreshToken token para renovar el acceso (puede usarse mas adelante)
 */
public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        String refreshToken) {
}
