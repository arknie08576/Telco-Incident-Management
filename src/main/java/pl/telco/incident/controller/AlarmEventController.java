package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventFilterRequest;
import pl.telco.incident.dto.AlarmEventPageResponse;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.dto.AlarmEventUpdateRequest;
import pl.telco.incident.service.AlarmEventService;

@Validated
@RestController
@RequestMapping("/api/alarm-events")
@RequiredArgsConstructor
@Tag(name = "Alarm Events")
public class AlarmEventController {

    private final AlarmEventService alarmEventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create alarm event", description = "Creates an alarm event and optionally correlates it with an incident.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alarm event created"),
            @ApiResponse(responseCode = "400", description = "Validation error",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Referenced incident or network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "Alarm event already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse createAlarmEvent(@Valid @RequestBody AlarmEventCreateRequest request) {
        return alarmEventService.createAlarmEvent(request);
    }

    @PatchMapping("/{id}")
    @Operation(summary = "Update alarm event", description = "Partially updates editable alarm fields such as status, incident correlation and maintenance suppression.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm event updated"),
            @ApiResponse(responseCode = "400", description = "Validation error or empty patch",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "Alarm event or referenced incident not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse updateAlarmEvent(
            @Parameter(description = "Alarm event identifier", example = "1")
            @PathVariable("id") Long id,
            @Valid @RequestBody AlarmEventUpdateRequest request
    ) {
        return alarmEventService.updateAlarmEvent(id, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alarm event by ID", description = "Returns a single alarm event.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm event found"),
            @ApiResponse(responseCode = "404", description = "Alarm event not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventResponse getAlarmEventById(
            @Parameter(description = "Alarm event identifier", example = "1")
            @PathVariable("id") Long id
    ) {
        return alarmEventService.getAlarmEventById(id);
    }

    @GetMapping
    @Operation(summary = "List alarm events", description = "Returns a paginated alarm event list with optional filters.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm events returned",
                    content = @Content(schema = @Schema(implementation = AlarmEventPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid filters or pagination values",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class)))
    })
    public AlarmEventPageResponse getAlarmEvents(@ParameterObject @ModelAttribute @Valid AlarmEventFilterRequest filter) {
        return AlarmEventPageResponse.from(alarmEventService.getAlarmEvents(filter));
    }
}
