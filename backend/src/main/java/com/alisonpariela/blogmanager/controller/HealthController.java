package com.alisonpariela.blogmanager.controller;

import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {
    private final JdbcTemplate database;

    public HealthController(JdbcTemplate database) { this.database = database; }

    @GetMapping("/healthz")
    public ResponseEntity<Map<String, String>> health() {
        try {
            if (Integer.valueOf(1).equals(database.queryForObject("select 1", Integer.class))) {
                return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("status", "ok"));
            }
        } catch (DataAccessException ignored) {
            // A readiness probe must not expose connection strings or credentials.
        }
        return ResponseEntity.status(503).cacheControl(CacheControl.noStore())
                .body(Map.of("status", "unavailable"));
    }
}
