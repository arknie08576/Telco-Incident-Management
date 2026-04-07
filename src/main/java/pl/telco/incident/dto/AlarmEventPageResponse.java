package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

@Getter
@Schema(name = "AlarmEventPageResponse", description = "Stable paginated response for alarm event listing.")
public class AlarmEventPageResponse {

    @ArraySchema(schema = @Schema(implementation = AlarmEventResponse.class))
    private final List<AlarmEventResponse> content;

    @Schema(description = "Zero-based page index.", example = "0")
    private final int number;

    @Schema(description = "Requested page size.", example = "10")
    private final int size;

    @Schema(description = "Total number of matching alarm events.", example = "25")
    private final long totalElements;

    @Schema(description = "Total number of pages.", example = "3")
    private final int totalPages;

    @Schema(description = "Whether this is the first page.", example = "true")
    private final boolean first;

    @Schema(description = "Whether this is the last page.", example = "false")
    private final boolean last;

    private AlarmEventPageResponse(
            List<AlarmEventResponse> content,
            int number,
            int size,
            long totalElements,
            int totalPages,
            boolean first,
            boolean last
    ) {
        this.content = content;
        this.number = number;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
    }

    public static AlarmEventPageResponse from(Page<AlarmEventResponse> page) {
        return new AlarmEventPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }
}
