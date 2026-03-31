package pl.telco.incident.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import pl.telco.incident.dto.AlarmEventCreateRequest;
import pl.telco.incident.dto.AlarmEventResponse;
import pl.telco.incident.service.AlarmEventService;

import java.util.List;

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

    @GetMapping
    @Operation(summary = "List alarm events", description = "Returns alarm events ordered by occurrence time descending.")
    @ApiResponse(responseCode = "200", description = "Alarm events returned",
            content = @Content(array = @ArraySchema(schema = @Schema(implementation = AlarmEventResponse.class))))
    public List<AlarmEventResponse> getAlarmEvents() {
        return alarmEventService.getAlarmEvents();
    }
}
