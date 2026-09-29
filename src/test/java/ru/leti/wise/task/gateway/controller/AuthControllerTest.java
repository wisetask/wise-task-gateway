package ru.leti.wise.task.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.graphql.types.ResetPasswordRequest;
import ru.leti.graphql.types.Role;
import ru.leti.graphql.types.SignInRequest;
import ru.leti.graphql.types.SignUpRequest;
import ru.leti.graphql.types.Token;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;

import static org.assertj.core.api.Assertions.assertThat;

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
}
