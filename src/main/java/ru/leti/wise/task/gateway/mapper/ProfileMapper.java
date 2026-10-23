package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.profile.ProfileGrpc;
import ru.leti.wise.task.profile.ProfileGrpc.ProfileFilter;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        uses = PaginationMapper.class)
public interface ProfileMapper {


    default ProfileFilter toProfileFilter(ProfileFilterInput filter) {
        if (filter == null) {
            return null;
        }
        var builder = ProfileFilter.newBuilder();
        if (filter.getEmail() != null) {
            builder.setEmail(filter.getEmail());
        }
        if (filter.getFirstName() != null) {
            builder.setFirstName(filter.getFirstName());
        }
        if (filter.getLastName() != null) {
            builder.setLastName(filter.getLastName());
        }
        if (filter.getPatronymic() != null) {
            builder.setPatronymic(filter.getPatronymic());
        }
        if (filter.getProfileRole() != null) {
            builder.addAllProfileRole(filter.getProfileRole());
        }
        return builder.build();
    }

    ProfileGrpc.GetAllProfilesRequest toGetAllRequest(GetAllProfilesRequestInput request);

    @Mapping(target = "items", source = "itemsList")
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
