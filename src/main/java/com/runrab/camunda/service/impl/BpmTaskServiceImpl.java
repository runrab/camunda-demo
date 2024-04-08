package com.runrab.camunda.service.impl;

import com.runrab.camunda.service.BpmTaskService;
import jakarta.annotation.Resource;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:17
 */
@Service
public class BpmTaskServiceImpl implements BpmTaskService {
    @Resource
    private TaskService taskService;
    @Override
    public void approveTask() {
        Task task = taskService.newTask();

    }
}
