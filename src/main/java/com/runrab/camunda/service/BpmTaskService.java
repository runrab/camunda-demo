package com.runrab.camunda.service;

import org.camunda.bpm.engine.history.HistoricTaskInstance;

import java.util.List;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:17
 */
public interface BpmTaskService {


    void approveTask(String taskId);
    void rejectTask(String taskId,String userId,String commit);

    void copyTask(String taskId, List<String> userList);

    void cancelTask(String taskId);

    void delegateTask(String taskId, String userId);

    void turnTask(String taskId, String userId);

    List<HistoricTaskInstance> getUserTask(String userId,String procInstId);
}
