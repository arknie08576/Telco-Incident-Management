package pl.telco.incident.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

@Schema(name = "MaintenanceWindowPageResponse", description = "Stable paginated response for maintenance window listing.")
public class MaintenanceWindowPageResponse {

    @ArraySchema(schema = @Schema(implementation = MaintenanceWindowResponse.class))
    private final List<MaintenanceWindowResponse> content;

    @Schema(description = "Zero-based page index.", example = "0")
    private final int number;

    @Schema(description = "Requested page size.", example = "10")
    private final int size;

    @Schema(description = "Total number of matching maintenance windows.", example = "12")
    private final long totalElements;

    @Schema(description = "Total number of pages.", example = "2")
    private final int totalPages;

    @Schema(description = "Whether this is the first page.", example = "true")
    private final boolean first;

    @Schema(description = "Whether this is the last page.", example = "false")
    private final boolean last;

    private MaintenanceWindowPageResponse(
            List<MaintenanceWindowResponse> content,
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

    public static MaintenanceWindowPageResponse from(Page<MaintenanceWindowResponse> page) {
        return new MaintenanceWindowPageResponse(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast()
        );
    }

    public List<MaintenanceWindowResponse> getContent() {
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
