package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import ru.leti.graphql.types.*;
import ru.leti.wise.task.profile.ProfileOuterClass;

import java.util.List;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface ProfileMapper {


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
