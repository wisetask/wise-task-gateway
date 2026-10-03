package ru.leti.wise.task.gateway.controller;

import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.graphql.types.ResetPasswordRequest;
import ru.leti.graphql.types.Role;
import ru.leti.graphql.types.SignInRequest;
import ru.leti.graphql.types.SignUpRequest;
import ru.leti.graphql.types.Token;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;
import ru.leti.wise.task.gateway.exception.InvalidRefreshTokenException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthControllerTest extends AbstractControllerTest {

    private AuthController controller() {
        return controller(AuthController.class);
    }

    @Test
    void signIn_returnsTokens() {
        profileGrpcService.putProfile(TestData.profile("user-1", "user@example.com"));

        var request = new SignInRequest();
        request.setEmail("user@example.com");
        request.setPassword("secret");

        Token token = controller().signIn(request);

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
    }

    @Test
    void signUp_returnsTokens() {
        var profile = new ProfileInput();
        profile.setId("user-2");
        profile.setEmail("new@example.com");
        profile.setProfilePassword("secret");
        profile.setFirstName("New");
        profile.setLastName("User");
        profile.setProfileRole(Role.USER);

        var request = new SignUpRequest();
        request.setProfile(profile);

        Token token = controller().signUp(request);

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
    }

    @Test
    void resetPassword_returnsTokens() {
        profileGrpcService.putProfile(TestData.profile("user-1", "user@example.com"));

        var request = new ResetPasswordRequest();
        request.setRecoveryToken("recovery-token");
        request.setNewPassword("new-secret");

        Token token = controller().resetPassword(request);

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
    }

    @Test
    void sendResetPasswordEmail_returnsEmailAndCallsService() {
        assertThat(controller().sendResetPasswordEmail("user@example.com")).isEqualTo("user@example.com");
        assertThat(profileGrpcService.getLastResetPasswordEmail()).isEqualTo("user@example.com");
    }

    @Test
    void changePassword_returnsProfileIdAndCallsService() {
        assertThat(controller().changePassword("user-1", "old", "new")).isEqualTo("user-1");
        assertThat(profileGrpcService.getLastChangedPasswordProfileId()).isEqualTo("user-1");
    }

    @Test
    void refreshToken_returnsNewAccessAndRefreshTokens() {
        profileGrpcService.putProfile(TestData.profile("user-1", "user@example.com"));
        jwtDecoder.putRefreshToken("refresh-token-1", "user-1");

        Token token = controller().refreshToken("refresh-token-1");

        assertThat(token.getAccessToken()).isNotBlank();
        assertThat(token.getRefreshToken()).isNotBlank();
        assertThat(token.getAccessToken()).isNotEqualTo(token.getRefreshToken());
    }

    @Test
    void refreshToken_issuesAccessTokenAndLongerLivedRefreshToken() {
        profileGrpcService.putProfile(TestData.profile("user-1", "user@example.com"));
        jwtDecoder.putRefreshToken("refresh-token-1", "user-1");

        controller().refreshToken("refresh-token-1");

        var claims = jwtEncoder.getEncodedClaims();
        assertThat(claims).hasSize(2);
        assertThat(claims.get(0).getSubject()).isEqualTo("user-1");
        assertThat((String) claims.get(0).getClaim("email")).isEqualTo("user@example.com");
        assertThat((Object) claims.get(0).getClaim("type")).isNull();
        assertThat((String) claims.get(1).getClaim("type")).isEqualTo("refresh");
        assertThat(claims.get(1).getExpiresAt()).isAfter(claims.get(0).getExpiresAt());
    }

    @Test
    void refreshToken_whenIssuerIsNotWiseTask_fails() {
        jwtDecoder.putTokenOfAnotherIssuer("google-token", "https://accounts.google.com", "user-1");

        assertThatThrownBy(() -> controller().refreshToken("google-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("issuer");
    }

    @Test
    void refreshToken_whenTokenIsNotRefreshToken_fails() {
        jwtDecoder.putAccessToken("access-token-1", "user-1", "user@example.com");

        assertThatThrownBy(() -> controller().refreshToken("access-token-1"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("refresh token");
    }

    @Test
    void refreshToken_whenTokenCannotBeDecoded_fails() {
        assertThatThrownBy(() -> controller().refreshToken("broken-token"))
                .isInstanceOf(InvalidRefreshTokenException.class)
                .hasMessageContaining("невалиден");
    }

    @Test
    void refreshToken_whenProfileNotFound_fails() {
        jwtDecoder.putRefreshToken("refresh-token-2", "missing-user");

        assertThatThrownBy(() -> controller().refreshToken("refresh-token-2"))
                .isInstanceOf(StatusRuntimeException.class);
    }

    @Test
    void refreshToken_incrementsMicrometerCounter() {
        profileGrpcService.putProfile(TestData.profile("user-1", "user@example.com"));
        jwtDecoder.putRefreshToken("refresh-token-1", "user-1");

        controller().refreshToken("refresh-token-1");

        assertThat(graphQlRequests("refreshToken")).isEqualTo(1);
    }
}
