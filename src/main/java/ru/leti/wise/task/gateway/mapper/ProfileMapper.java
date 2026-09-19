package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.profile.ProfileGrpc;
import ru.leti.wise.task.profile.ProfileGrpc.ProfileFilter;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        uses = PaginationMapper.class)
public interface ProfileMapper {


    ProfileFilter toProfileFilter(ProfileFilterInput filter);

    ProfileGrpc.GetAllProfilesRequest toGetAllRequest(GetAllProfilesRequestInput request);

    GetAllProfilesResponse toGetAllResponse(ProfileGrpc.GetAllProfilesResponse response);

    ProfileOuterClass.Profile toProfile(ProfileInput profile);

    Profile toProfile(ProfileOuterClass.Profile profile);

    List<Profile> toProfiles(List<ProfileOuterClass.Profile> profiles);

    default ProfileOuterClass.Role toRole(Role role) {
        return ProfileOuterClass.Role.valueOf(role.name());
    }

    default Role toRole(ProfileOuterClass.Role role) {
        return Role.valueOf(role.name());
    }


}
