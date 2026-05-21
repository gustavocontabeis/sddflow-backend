package com.example.springia.controller;

import com.example.springia.dto.CreateSessionRequest;
import com.example.springia.dto.SpecificationResponse;
import com.example.springia.model.Message;
import com.example.springia.model.UserStory;
import com.example.springia.service.ChatService;
import com.example.springia.service.MessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/message")
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    @PostMapping()
    public Message post(@RequestBody Message request) {
        return messageService.save(request);
    }

}