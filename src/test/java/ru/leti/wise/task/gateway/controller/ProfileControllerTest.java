package ru.leti.wise.task.gateway.controller;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.GetAllProfilesRequestInput;
import ru.leti.graphql.types.Profile;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.graphql.types.Role;
import ru.leti.wise.task.gateway.controller.support.AbstractControllerTest;
import ru.leti.wise.task.gateway.controller.support.TestData;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileControllerTest extends AbstractControllerTest {

    private ProfileController controller() {
        return controller(ProfileController.class);
    }

    @Test
    void getProfile_mapsAllFields() {
        profileGrpcService.putProfile(TestData.profile("profile-1", "user@example.com"));

        Profile profile = controller().getProfile("profile-1");

        assertThat(profile.getId()).isEqualTo("profile-1");
        assertThat(profile.getEmail()).isEqualTo("user@example.com");
        assertThat(profile.getFirstName()).isEqualTo("First profile-1");
        assertThat(profile.getLastName()).isEqualTo("Last profile-1");
        assertThat(profile.getPatronymic()).isEqualTo("Patronymic profile-1");
        assertThat(profile.getProfileRole()).isEqualTo(Role.AUTHOR);
    }

    @Test
    void getAllProfiles_mapsItemsAndPagination() {
        profileGrpcService.putProfile(TestData.profile("profile-1", "user1@example.com"));
        profileGrpcService.putProfile(TestData.profile("profile-2", "user2@example.com"));

        var response = controller().getAllProfiles(GetAllProfilesRequestInput.newBuilder().build());

        assertThat(response.getItems()).hasSize(2);
        assertThat(response.getPagination().getTotalCount()).isEqualTo(1L);
        assertThat(response.getItems().get(0).getEmail()).isEqualTo("user1@example.com");
        assertThat(response.getItems().get(1).getEmail()).isEqualTo("user2@example.com");
    }

    @Test
    void updateProfile_mapsUpdatedProfile() {
        var input = new ProfileInput();
        input.setId("profile-1");
        input.setEmail("updated@example.com");
        input.setProfilePassword("secret");
        input.setFirstName("Updated");
        input.setLastName("User");
        input.setPatronymic("Patronymic");
        input.setProfileRole(Role.ADMIN);

        Profile updated = controller().updateProfile(input);

        assertThat(updated.getId()).isEqualTo("profile-1");
        assertThat(updated.getEmail()).isEqualTo("updated@example.com");
        assertThat(updated.getFirstName()).isEqualTo("Updated");
        assertThat(updated.getLastName()).isEqualTo("User");
        assertThat(updated.getPatronymic()).isEqualTo("Patronymic");
        assertThat(updated.getProfileRole()).isEqualTo(Role.ADMIN);
    }

    @Test
    void deleteProfile_returnsDeletedId() {
        profileGrpcService.putProfile(TestData.profile("profile-1", "user@example.com"));

        assertThat(controller().deleteProfile("profile-1")).isEqualTo("profile-1");
    }
}
