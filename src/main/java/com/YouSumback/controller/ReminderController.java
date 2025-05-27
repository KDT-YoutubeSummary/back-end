package com.YouSumback.controller;

import com.YouSumback.service.ReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReminderController {
    @Autowired
    ReminderService reminderService;
}
