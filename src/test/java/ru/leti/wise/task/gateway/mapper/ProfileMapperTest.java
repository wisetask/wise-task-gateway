package ru.leti.wise.task.gateway.mapper;

import org.junit.jupiter.api.Test;
import ru.leti.graphql.types.GetAllProfilesRequestInput;
import ru.leti.graphql.types.PaginationRequestInput;
import ru.leti.graphql.types.ProfileFilterInput;
import ru.leti.wise.task.profile.ProfileGrpc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileMapperTest {

    private final ProfileMapper mapper = new ProfileMapperImpl();

    @Test
    void filterCarriesProfileRolesToGrpc() {
        var filter = new ProfileFilterInput();
        filter.setEmail("a@b.com");
        filter.setFirstName("First");
        filter.setLastName("Last");
        filter.setPatronymic("Patronymic");
        filter.setProfileRole(List.of("ADMIN", "USER"));

        var grpcFilter = mapper.toProfileFilter(filter);

        assertThat(grpcFilter.getEmail()).isEqualTo("a@b.com");
        assertThat(grpcFilter.getFirstName()).isEqualTo("First");
        assertThat(grpcFilter.getLastName()).isEqualTo("Last");
        assertThat(grpcFilter.getPatronymic()).isEqualTo("Patronymic");
        assertThat(grpcFilter.getProfileRoleList()).containsExactly("ADMIN", "USER");
    }

    @Test
    void filterWithoutRolesHasEmptyRoleList() {
        var filter = new ProfileFilterInput();
        filter.setEmail("a@b.com");

        var grpcFilter = mapper.toProfileFilter(filter);

        assertThat(grpcFilter.getProfileRoleList()).isEmpty();
        assertThat(grpcFilter.getEmail()).isEqualTo("a@b.com");
    }

    @Test
    void nullFilterMapsToNull() {
        assertThat(mapper.toProfileFilter(null)).isNull();
    }

    @Test
    void getAllRequestCarriesPaginationAndFilter() {
        var pagination = new PaginationRequestInput();
        pagination.setPage(3);
        pagination.setPageSize(25);
        var filter = new ProfileFilterInput();
        filter.setProfileRole(List.of("AUTHOR"));
        var request = new GetAllProfilesRequestInput();
        request.setPagination(pagination);
        request.setFilter(filter);

        var grpcRequest = mapper.toGetAllRequest(request);

        assertThat(grpcRequest.getPagination().getPage()).isEqualTo(3);
        assertThat(grpcRequest.getPagination().getPageSize()).isEqualTo(25);
        assertThat(grpcRequest.getFilter().getProfileRoleList()).containsExactly("AUTHOR");
    }

    @Test
    void emptyRequestMapsToDefaults() {
        var grpcRequest = mapper.toGetAllRequest(new GetAllProfilesRequestInput());

        assertThat(grpcRequest.getPagination().getPage()).isEqualTo(0);
        assertThat(grpcRequest.getPagination().getPageSize()).isEqualTo(0);
        assertThat(grpcRequest.hasFilter()).isFalse();
    }
}
