package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.NullValueCheckStrategy;
import ru.leti.graphql.types.PaginationResponse;
import ru.leti.wise.task.pagination.Pagination;

@Mapper(componentModel = "spring", nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS)
public interface PaginationMapper {
    PaginationResponse toPagination(Pagination.PaginationResponse pagination);
}
