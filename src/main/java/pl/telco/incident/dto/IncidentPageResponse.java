package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "IncidentPageResponse", description = "Stable paginated response for incident listing.")
public class IncidentPageResponse {

    @ArraySchema(schema = @Schema(implementation = IncidentResponse.class))
    private final List<IncidentResponse> content;

    @Schema(description = "Zero-based page index.", example = "1")
    private final int number;

    @Schema(description = "Requested page size.", example = "10")
    private final int size;

    @Schema(description = "Total number of matching incidents.", example = "42")
    private final long totalElements;

    @Schema(description = "Total number of pages.", example = "5")
    private final int totalPages;

    @Schema(description = "Whether this is the first page.", example = "false")
    private final boolean first;

    @Schema(description = "Whether this is the last page.", example = "false")
    private final boolean last;

    private IncidentPageResponse(
            List<IncidentResponse> content,
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

    public static IncidentPageResponse from(Page<IncidentResponse> page) {
        return new IncidentPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public List<IncidentResponse> getContent() {
        return content;
    }

    public int getNumber() {
        return number;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isFirst() {
        return first;
    }

    public boolean isLast() {
        return last;
    }
}
