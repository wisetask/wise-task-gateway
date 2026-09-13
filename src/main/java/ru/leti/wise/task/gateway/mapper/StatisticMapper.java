package ru.leti.wise.task.gateway.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.NullValueCheckStrategy;
import org.mapstruct.NullValuePropertyMappingStrategy;
import ru.leti.wise.task.event.Statistic;

import com.google.protobuf.Timestamp;
import ru.leti.graphql.types.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Mapper(
        componentModel = "spring",
        nullValueCheckStrategy = NullValueCheckStrategy.ALWAYS,
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface StatisticMapper {

    @Mapping(target = "eventType", source = "event_type")
    @Mapping(target = "userId", source = "user_id")
    @Mapping(target = "taskId", source = "task_id")
    @Mapping(target = "type", expression = "java(mapType(request.getType()))")
    @Mapping(target = "scope", expression = "java(mapScope(request.getScope()))")
    Statistic.StatisticRequest toStatistic(StatisticRequestInput request);

    @Mapping(target = "scope", expression = "java(mapScope(response.getScope()))")
    @Mapping(target = "type", expression = "java(mapType(response.getType()))")
    @Mapping(target = "event_type", source = "eventType")
    @Mapping(target = "user_id", source = "userId")
    @Mapping(target = "task_id", source = "taskId")
    @Mapping(target = "updated_at", expression = "java(formatTimestamp(response.getUpdateAt()))")
    @Mapping(target = "value", source = "value")
    StatisticResponse toStatistic(Statistic.StatisticResponse response);

    default String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) {
            return LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        Instant instant = Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos());

        return LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
    }


    default Statistic.StatisticType mapType(StatisticType type) {
        if (type == null) {
            return null;
        }
        return Statistic.StatisticType.valueOf(type.name());
    }

    default StatisticType mapType(Statistic.StatisticType type) {
        if (type == null) {
            return null;
        }
        return StatisticType.valueOf(type.name());
    }

    default Statistic.StatisticScope mapScope(StatisticScope scope) {
        if (scope == null) {
            return null;
        }
        return Statistic.StatisticScope.valueOf(scope.name());
    }

    default StatisticScope mapScope(Statistic.StatisticScope scope) {
        if (scope == null) {
            return null;
        }
        return StatisticScope.valueOf(scope.name());
    }
}
