package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import ru.leti.graphql.types.GetAllProfilesRequestInput;
import ru.leti.graphql.types.GetAllProfilesResponse;
import ru.leti.graphql.types.Profile;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.service.grpc.ProfileGrpcService;
import ru.leti.wise.task.gateway.service.grpc.TaskGrpcService;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileGrpcService profileGrpcService;
    private final ProfileMapper profileMapper;
    @Lazy @Autowired
    private ProfileService self;

    @Cacheable(value = "profile", key = "#userId")
    public Profile getProfile(String userId) {
        var grpcResponse = profileGrpcService.getProfile(userId);
        return profileMapper.toProfile(grpcResponse);
    }

    public Profile getProfileByEmail(String email) {
        String userId = self.resolveUserIdByEmail(email);
        return self.getProfile(userId);
    }

    @Cacheable(value = "profile-by-email", key = "#email", cacheManager = "localCacheManager")
    public String resolveUserIdByEmail(String email) {
        return profileGrpcService.getProfileByEmail(email).getId();
    }

    public GetAllProfilesResponse getAllProfiles(GetAllProfilesRequestInput request) {
        var grpcRequest = profileMapper.toGetAllRequest(request);
        var grpcResponse = profileGrpcService.getAllProfiles(grpcRequest);
        return profileMapper.toGetAllResponse(grpcResponse);
    }

    @CachePut(
            value = "profile",
            key = "#profile.id",
            cacheManager = "cacheManager"
    )
    public Profile updateProfile(ProfileInput profile) {
        var grpcRequest = profileMapper.toProfile(profile);
        var grpcResponse = profileGrpcService.updateProfile(grpcRequest);
        return profileMapper.toProfile(grpcResponse);
    }

    @CacheEvict(
            value = "profile",
            key = "#userId",
            cacheManager = "cacheManager"
    )
    public String deleteProfile(String userId) {
        profileGrpcService.deleteProfile(userId);
        return userId;
    }
}
