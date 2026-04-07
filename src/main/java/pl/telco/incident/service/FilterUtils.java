package pl.telco.incident.service;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.springframework.data.domain.Sort;
import pl.telco.incident.exception.BadRequestException;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
final class FilterUtils {

    static void validateSortBy(String sortBy, Map<String, String> allowedFields) {
        if (!allowedFields.containsKey(sortBy)) {
            throw new BadRequestException("Unsupported sortBy value: " + sortBy);
        }
    }

    static void validateSortBy(String sortBy, Set<String> allowedFields) {
        if (!allowedFields.contains(sortBy)) {
            throw new BadRequestException("Unsupported sortBy value: " + sortBy);
        }
    }

    static Sort.Direction parseSortDirection(String direction) {
        try {
            return Sort.Direction.fromString(direction);
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Unsupported direction value: " + direction);
        }
    }

    static Sort buildSort(String sortBy, Sort.Direction direction, Map<String, String> allowedFields) {
        Sort sort = Sort.by(direction, allowedFields.get(sortBy));
        if (!"id".equals(sortBy)) {
            sort = sort.and(Sort.by(Sort.Direction.DESC, "id"));
        }
        return sort;
    }

    static <E extends Enum<E>> Set<E> parseEnumFilters(List<String> rawValues, Class<E> enumType, String parameterName) {
        LinkedHashSet<E> parsedValues = new LinkedHashSet<>();

        if (rawValues == null) {
            return parsedValues;
        }

        for (String rawValue : rawValues) {
            if (rawValue == null || rawValue.isBlank()) {
                continue;
            }

            for (String token : rawValue.split(",")) {
                String normalizedToken = token.trim();
                if (normalizedToken.isEmpty()) {
                    continue;
                }

                try {
                    parsedValues.add(Enum.valueOf(enumType, normalizedToken.toUpperCase(Locale.ROOT)));
                } catch (IllegalArgumentException ex) {
                    throw new BadRequestException("Invalid value '%s' for parameter '%s'".formatted(normalizedToken, parameterName));
                }
            }
        }

        return parsedValues;
    }

    static <E extends Enum<E>> Set<E> mergeEnumFilters(E singleValue, List<String> multiValues, Class<E> enumType, String parameterName) {
        LinkedHashSet<E> merged = new LinkedHashSet<>();

        if (singleValue != null) {
            merged.add(singleValue);
        }
        merged.addAll(parseEnumFilters(multiValues, enumType, parameterName));

        return merged;
    }
}
