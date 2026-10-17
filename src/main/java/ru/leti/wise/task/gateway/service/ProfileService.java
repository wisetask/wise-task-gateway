package ru.leti.wise.task.gateway.service;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import ru.leti.graphql.types.GetAllProfilesRequestInput;
import ru.leti.graphql.types.GetAllProfilesResponse;
import ru.leti.graphql.types.Profile;
import ru.leti.graphql.types.ProfileInput;
import ru.leti.wise.task.gateway.mapper.ProfileMapper;
import ru.leti.wise.task.gateway.service.grpc.ProfileGrpcService;

@Service
@RequiredArgsConstructor
public class ProfileService {
    private final ProfileGrpcService profileGrpcService;
    private final ProfileMapper profileMapper;

    @Cacheable(value = "profile", key = "#userId", cacheManager = "cacheManager")
    public Profile getProfile(String userId) {
        var grpcResponse = profileGrpcService.getProfile(userId);
        return profileMapper.toProfile(grpcResponse);
    }

    @Cacheable(value = "profile", key = "#email", cacheManager = "cacheManager")
    public Profile getProfileByEmail(String email) {
        var grpcResponse = profileGrpcService.getProfileByEmail(email);
        return profileMapper.toProfile(grpcResponse);
    }

    public GetAllProfilesResponse getAllProfiles(GetAllProfilesRequestInput request) {
        var grpcRequest = profileMapper.toGetAllRequest(request);
        var grpcResponse = profileGrpcService.getAllProfiles(grpcRequest);
        return profileMapper.toGetAllResponse(grpcResponse);
    }

    @CachePut(
            value = "profile",
            key = "#profile.email",
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
    public String deleteProfile(String id) {
        profileGrpcService.deleteProfile(id);
        return id;
    }
}
