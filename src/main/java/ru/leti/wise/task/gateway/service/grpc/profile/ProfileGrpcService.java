package ru.leti.wise.task.gateway.service.grpc.profile;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.profile.ProfileGrpc;
import ru.leti.wise.task.profile.ProfileGrpc.GetAllProfilesRequest;
import ru.leti.wise.task.profile.ProfileGrpc.GetAllProfilesResponse;
import ru.leti.wise.task.profile.ProfileOuterClass.Profile;
import ru.leti.wise.task.profile.ProfileServiceGrpc.ProfileServiceBlockingStub;

import java.util.List;


@Slf4j
@Component
@Observed
@RequiredArgsConstructor
public class ProfileGrpcService {

    private final ProfileServiceBlockingStub profileService;

    public GetAllProfilesResponse getAllProfiles(GetAllProfilesRequest request) {
        log.debug("ProfileService.getAllProfiles request, {}", request);
        var response = profileService.getAllProfiles(request);
        log.debug("ProfileService.getAllProfiles request, fetched {} profiles", response.getItemsCount());
        return response;
    }

    public Profile getProfile(String id) {
        log.debug("ProfileService.getProfile request, profile id {}", id);
        var request = ProfileGrpc.GetProfileRequest.newBuilder()
                .setProfileId(id)
                .build();

        var profile = profileService.getProfile(request).getProfile();
        log.debug("ProfileService.getProfile request, fetched profile {}", profile.getId());
        return profile;
    }

    public Profile getProfileByEmail(String email) {
        log.debug("ProfileService.getProfileByEmail request, email {}", email);
        var request = ProfileGrpc.GetProfileByEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        var profile = profileService.getProfileByEmail(request).getProfile();
        log.debug("ProfileService.getProfileByEmail request, fetched profile {}", profile.getId());
        return profile;
    }

    public void deleteProfile(String id) {
        log.debug("ProfileService.deleteProfile request, profile id {}", id);
        var request = ProfileGrpc.DeleteProfileRequest.newBuilder()
                .setProfileId(id)
                .build();

        profileService.deleteProfile(request);
    }

    public Profile updateProfile(Profile profile) {
        log.debug("ProfileService.updateProfile request, profile id {}", profile.getId());
        var request = ProfileGrpc.UpdateProfileRequest.newBuilder()
                .setProfile(profile)
                .build();

        var updated = profileService.updateProfile(request).getProfile();
        log.debug("ProfileService.updateProfile request, updated profile {}", updated.getId());
        return updated;
    }

    public Profile signIn(String email, String password) {
        log.debug("ProfileService.signIn request, email {}", email);
        var request = ProfileGrpc.SignInRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        var profile = profileService.signIn(request).getProfile();
        log.debug("ProfileService.signIn request, signed in profile {}", profile.getId());
        return profile;
    }

    public Profile signUp(Profile profile, Boolean isExternal) {
        log.debug("ProfileService.signUp request, email {}", profile.getEmail());
        var request = ProfileGrpc.SignUpRequest.newBuilder()
                .setProfile(profile)
                .setIsExternal(isExternal)
                .build();

        var created = profileService.signUp(request).getProfile();
        log.debug("ProfileService.signUp request, signed up profile {}", created.getId());
        return created;
    }

    public Profile resetPassword(String recoveryToken, String newPassword) {
        log.debug("ProfileService.resetPassword request, resetting password");
        var request = ProfileGrpc.ResetPasswordRequest.newBuilder()
                .setRecoveryToken(recoveryToken)
                .setNewPassword(newPassword)
                .build();

        var profile = profileService.resetPassword(request).getProfile();
        log.debug("ProfileService.resetPassword request, reset password for profile {}", profile.getId());
        return profile;
    }

    public void sendResetPasswordEmail(String email) {
        log.debug("ProfileService.sendResetPasswordEmail request, email {}", email);
        var request = ProfileGrpc.SendResetPasswordEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        profileService.sendResetPasswordEmail(request);
    }

    public void changePassword(String id, String oldPassword, String newPassword) {
        log.debug("ProfileService.changePassword request, profile id {}", id);
        var request = ProfileGrpc.ChangePasswordRequest.newBuilder().setProfileId(id)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword)
                .build();

        profileService.changePassword(request);
    }

    public List<Profile> getProfilesByIds(List<String> profileIds) {
        log.debug("ProfileService.getProfilesByIds request, profile ids {}", profileIds);
        var request = ProfileGrpc.ProfileIds.newBuilder().addAllProfileIds(profileIds).build();
        var profiles = profileService.getProfilesByIds(request).getProfilesList();
        log.debug("ProfileService.getProfilesByIds request, fetched {} profiles", profiles.size());
        return profiles;
    }
}
