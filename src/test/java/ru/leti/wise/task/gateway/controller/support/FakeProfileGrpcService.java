package ru.leti.wise.task.gateway.controller.support;

import io.grpc.Status;
import ru.leti.wise.task.gateway.service.grpc.profile.ProfileGrpcService;
import ru.leti.wise.task.profile.ProfileGrpc.GetAllProfilesRequest;
import ru.leti.wise.task.profile.ProfileGrpc.GetAllProfilesResponse;
import ru.leti.wise.task.profile.ProfileOuterClass.Profile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Фейковый profile-сервис: отдаёт заранее подготовленные профили, умеет имитировать gRPC-ошибки.
 */
public class FakeProfileGrpcService extends ProfileGrpcService {

    private final Map<String, Profile> profiles = new LinkedHashMap<>();
    private boolean failGetProfile;
    private boolean failGetProfilesByIds;
    private String lastResetPasswordEmail;
    private String lastChangedPasswordProfileId;

    public FakeProfileGrpcService() {
        super(null);
    }

    public void putProfile(Profile profile) {
        profiles.put(profile.getId(), profile);
    }

    public void failGetProfile() {
        failGetProfile = true;
    }

    public void failGetProfilesByIds() {
        failGetProfilesByIds = true;
    }

    public String getLastResetPasswordEmail() {
        return lastResetPasswordEmail;
    }

    public String getLastChangedPasswordProfileId() {
        return lastChangedPasswordProfileId;
    }

    @Override
    public GetAllProfilesResponse getAllProfiles(GetAllProfilesRequest request) {
        return GetAllProfilesResponse.newBuilder()
                .addAllItems(profiles.values())
                .setPagination(TestData.pagination())
                .build();
    }

    @Override
    public Profile getProfile(String id) {
        if (failGetProfile) {
            throw Status.UNAVAILABLE.withDescription("profile service unavailable").asRuntimeException();
        }
        var profile = profiles.get(id);
        if (profile == null) {
            throw Status.NOT_FOUND.withDescription("profile " + id + " not found").asRuntimeException();
        }
        return profile;
    }

    @Override
    public List<Profile> getProfilesByIds(List<String> profileIds) {
        if (failGetProfilesByIds) {
            throw Status.UNAVAILABLE.withDescription("profile service unavailable").asRuntimeException();
        }
        return profileIds.stream().map(profiles::get).filter(Objects::nonNull).toList();
    }

    @Override
    public void deleteProfile(String id) {
        profiles.remove(id);
    }

    @Override
    public Profile updateProfile(Profile profile) {
        profiles.put(profile.getId(), profile);
        return profile;
    }

    @Override
    public Profile signIn(String email, String password) {
        return profiles.values().stream()
                .filter(profile -> profile.getEmail().equals(email))
                .findFirst()
                .orElseThrow(() -> Status.UNAUTHENTICATED
                        .withDescription("bad credentials for " + email)
                        .asRuntimeException());
    }

    @Override
    public Profile signUp(Profile profile, Boolean isExternal) {
        profiles.put(profile.getId(), profile);
        return profile;
    }

    @Override
    public Profile resetPassword(String recoveryToken, String newPassword) {
        return profiles.values().stream()
                .findFirst()
                .orElseThrow(() -> Status.NOT_FOUND
                        .withDescription("profile for token " + recoveryToken + " not found")
                        .asRuntimeException());
    }

    @Override
    public void sendResetPasswordEmail(String email) {
        this.lastResetPasswordEmail = email;
    }

    @Override
    public void changePassword(String id, String oldPassword, String newPassword) {
        this.lastChangedPasswordProfileId = id;
    }
}
