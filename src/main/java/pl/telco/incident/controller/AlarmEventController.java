package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.AlarmEventRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.service.AlarmEventService;

import java.util.List;

@RestController
@RequestMapping("/api/alarm-events")
@RequiredArgsConstructor
@Tag(name = "Alarm Events")
public class AlarmEventController {

    private final AlarmEventService alarmEventService;

    @GetMapping
    @Operation(summary = "List alarm events")
    public List<AlarmEventResponse> getAllAlarmEvents() {
        return alarmEventService.getAllAlarmEvents();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get alarm event by ID")
    public AlarmEventResponse getAlarmEventById(@PathVariable("id") Long id) {
        return alarmEventService.getAlarmEventById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create alarm event")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Alarm event created"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid timestamp order or incident/network node mismatch",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Referenced incident or network node not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Alarm with the same sourceSystem/externalId already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public AlarmEventResponse createAlarmEvent(@Valid @RequestBody AlarmEventRequest request) {
        return alarmEventService.createAlarmEvent(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update alarm event")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Alarm event updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid timestamp order or incident/network node mismatch",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "404",
                    description = "Alarm event or referenced entities not found",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Alarm with the same sourceSystem/externalId already exists",
                    content = @Content(schema = @Schema(implementation = pl.telco.incident.exception.ApiErrorResponse.class))
            )
    })
    public AlarmEventResponse updateAlarmEvent(
            @PathVariable("id") Long id,
            @Valid @RequestBody AlarmEventRequest request
    ) {
        return alarmEventService.updateAlarmEvent(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete alarm event")
    public void deleteAlarmEvent(@PathVariable("id") Long id) {
        alarmEventService.deleteAlarmEvent(id);
    }
}
