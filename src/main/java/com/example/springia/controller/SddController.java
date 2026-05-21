package com.example.springia.controller;

import com.example.springia.service.SddService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/sdd")
@RequiredArgsConstructor
public class SddController {

    private final SddService sddService;

    @PostMapping("/spec")
    public String createSpec(@RequestParam Long sessionId) {
        return sddService.createSpec(sessionId);
    }

    @PostMapping("/plan")
    public String createPlan(@RequestParam Long sessionId) {
        return sddService.createPlan(sessionId);
    }

    @PostMapping("/task")
    public String createTask(@RequestParam Long sessionId) {
        return sddService.createTask(sessionId);
    }

    @PostMapping("/impl")
    public String createImpl(@RequestParam Long sessionId) {
        return sddService.createImpl(sessionId);
    }

}