package ru.leti.wise.task.gateway.service.grpc.profile;

import com.google.protobuf.Empty;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.leti.wise.task.profile.ProfileGrpc;
import ru.leti.wise.task.profile.ProfileOuterClass.Profile;
import ru.leti.wise.task.profile.ProfileServiceGrpc.ProfileServiceBlockingStub;

import java.util.List;


@Component
@Observed
@RequiredArgsConstructor
public class ProfileGrpcService {

    private final ProfileServiceBlockingStub profileService;

    public List<Profile> getAllProfiles() {
        var request = Empty.newBuilder().build();

        return profileService.getAllProfiles(request).getProfileList();
    }

    public Profile getProfile(String id) {
        var request = ProfileGrpc.GetProfileRequest.newBuilder()
                .setProfileId(id)
                .build();

        return profileService.getProfile(request).getProfile();
    }

    public Profile getProfileByEmail(String email) {
        var request = ProfileGrpc.GetProfileByEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        return profileService.getProfileByEmail(request).getProfile();
    }

    public void deleteProfile(String id) {
        var request = ProfileGrpc.DeleteProfileRequest.newBuilder()
                .setProfileId(id)
                .build();

        profileService.deleteProfile(request);
    }

    public Profile updateProfile(Profile profile) {
        var request = ProfileGrpc.UpdateProfileRequest.newBuilder()
                .setProfile(profile)
                .build();

        return profileService.updateProfile(request).getProfile();
    }

    public Profile signIn(String email, String password) {
        var request = ProfileGrpc.SignInRequest.newBuilder()
                .setEmail(email)
                .setPassword(password)
                .build();

        return profileService.signIn(request).getProfile();
    }

    public Profile signUp(Profile profile) {
        var request = ProfileGrpc.SignUpRequest.newBuilder()
                .setProfile(profile)
                .build();

        return profileService.signUp(request).getProfile();
    }

    public Profile resetPassword(String recoveryToken, String newPassword) {
        var request = ProfileGrpc.ResetPasswordRequest.newBuilder()
                .setRecoveryToken(recoveryToken)
                .setNewPassword(newPassword)
                .build();

        return profileService.resetPassword(request).getProfile();
    }

    public void sendResetPasswordEmail(String email) {
        var request = ProfileGrpc.SendResetPasswordEmailRequest.newBuilder()
                .setEmail(email)
                .build();

        profileService.sendResetPasswordEmail(request);
    }

    public void changePassword(String id, String oldPassword, String newPassword) {
        var request = ProfileGrpc.ChangePasswordRequest.newBuilder().setProfileId(id)
                .setOldPassword(oldPassword)
                .setNewPassword(newPassword)
                .build();

        profileService.changePassword(request);
    }


}
