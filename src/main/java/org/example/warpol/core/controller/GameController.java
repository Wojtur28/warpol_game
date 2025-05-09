package org.example.warpol.core.controller;

import lombok.RequiredArgsConstructor;
import org.example.warpol.core.dto.ExecuteCommandRequest;
import org.example.warpol.core.dto.PlayerCommandRequest;
import org.example.warpol.core.dto.UnitResponse;
import org.example.warpol.core.entity.type.PlayerColor;
import org.example.warpol.core.entity.unit.Unit;
import org.example.warpol.core.service.GameService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class GameController {

    private final GameService gameService;

    @PostMapping("/game/new")
    public ResponseEntity<String> createNewGame() {
        gameService.createNewGameFromConfig();
        return ResponseEntity.ok("New game started");
    }

    @GetMapping("/units")
    public ResponseEntity<List<UnitResponse>> getUnits(@RequestParam PlayerColor color) {
        List<Unit> units = gameService.getUnits(color);
        List<UnitResponse> response = units.stream().map(UnitResponse::from).toList();
        return ResponseEntity.ok(response);
    }

    @PostMapping("/command/execute")
    public ResponseEntity<String> executeCommand(@RequestBody PlayerCommandRequest<ExecuteCommandRequest> request) {
        gameService.executeCommand(
                request.command().unitId(),
                request.command().commandType(),
                request.command().targetX(),
                request.command().targetY(),
                request.playerColor()
        );
        return ResponseEntity.ok("Command executed");
    }

    @PostMapping("/command/random")
    public ResponseEntity<String> executeRandomCommand(@RequestBody PlayerCommandRequest<UUID> request) {
        gameService.executeRandomCommand(request.playerColor(), request.command());
        return ResponseEntity.ok("Random command executed");
    }
}

