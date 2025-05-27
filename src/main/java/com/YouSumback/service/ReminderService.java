package com.YouSumback.service;

import com.YouSumback.repository.ReminderRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ReminderService {
    @Autowired
    ReminderRepository reminderRepository;


}
