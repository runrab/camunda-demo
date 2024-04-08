package com.runrab.camunda.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.history.event.*;
import org.camunda.bpm.spring.boot.starter.event.*;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 功能描述：工作流eventBus监听
 *
 * @author runrab
 * @date 2024/4/8 22:22
 */
@Component
@Slf4j
public class EventSubscriber {


    /**
     * 任务监听器事件
     */
    @EventListener
    public void handleTaskEvent(TaskEvent event) {
        String eventName = event.getEventName();
        String processDefinitionId = event.getProcessDefinitionId();
        // 处理任务事件 multiInstanceBody
        log.info("处理任务事件: {}", event.getEventName());

        switch (eventName) {
            case TaskListener.EVENTNAME_CREATE:
                break;
            case TaskListener.EVENTNAME_ASSIGNMENT:
                log.info("分配具体人员");
                break;
            case TaskListener.EVENTNAME_COMPLETE:
                log.info("完成任务");
                break;
            case TaskListener.EVENTNAME_DELETE:
                break;
            case TaskListener.EVENTNAME_UPDATE:
                break;
            case TaskListener.EVENTNAME_TIMEOUT:
                break;
        }

    }


    /**
     * 执行监听器事件
     */
    @EventListener
    public void handleExecutionEvent(ExecutionEvent event) {
        // 处理执行事件
        log.info("处理执行事件: {}", event.getEventName());

    }

    /**
     * 历史事件
     * HistoricTaskInstanceEventEntity 任务
     */
    @EventListener
    public void handleHistoryEvent(HistoryEvent event) {

        String eventType = event.getEventType();
        //
        HistoryEventTypes taskInstanceComplete = HistoryEventTypes.TASK_INSTANCE_COMPLETE;
        Object persistentState = event.getPersistentState();
        if (TaskListener.EVENTNAME_COMPLETE.equals(eventType)) {
            // 完成任务
            log.info("TaskListener.EVENTNAME_COMPLETE");
        } else if (TaskListener.EVENTNAME_DELETE.equals(eventType)) {
            log.info("EVENTNAME_DELETE");
        } else if (TaskListener.EVENTNAME_ASSIGNMENT.equals(eventType)) {
            log.info("EVENTNAME_ASSIGNMENT");
        } else if (HistoryEventTypes.ACTIVITY_INSTANCE_START.getEventName().equals(eventType)) {
            //发起流程
            log.info("ACTIVITY_INSTANCE_START");
            // 需要判断是执行监听器的start还是流程的start
        }

        // 流程的
        if (event instanceof HistoricProcessInstanceEventEntity) {

            log.info("ACTIVITY_INSTANCE_");
        } else if (event instanceof HistoricActivityInstanceEventEntity) {
            // 节点的 第一个是开始节点 activityType 是 startEvent   第二个是 activityType  : userTask
            log.info("ACTIVITY_INSTANCE_");
        } else if (event instanceof HistoricVariableUpdateEventEntity) {
            // 变量更新
        }


    }


    @EventListener
    public void handleHistoryEvent(HistoricTaskInstanceEventEntity event) {
        String eventType = event.getEventType();
        String caseInstanceId = event.getCaseInstanceId();
        String owner = event.getOwner();// 任务的抄送人
        String assignee = event.getAssignee();// 任务的接收人
        // 抄送任务 caseInstId 不为空
        if (TaskListener.EVENTNAME_CREATE.equals(eventType) && StringUtils.isNotEmpty(caseInstanceId)) {


        }
    }

    /**
     * 监听流程
     */
    @EventListener
    public void handleHistoryEvent(HistoricProcessInstanceEventEntity event) {
        String eventType = event.getEventType();
        String state = event.getState();// ACTIVE INTERNALLY_TERMINATED EXTERNALLY_TERMINATED
        String deleteReason = event.getDeleteReason();// deleted/completed

        // 流程开始
        if (HistoryEventTypes.ACTIVITY_INSTANCE_START.getEventName().equals(eventType)) {

        } else if (HistoryEventTypes.ACTIVITY_INSTANCE_END.getEventName().equals(eventType)) {
            // 接口调用的方式删除流程实例
            if ("EXTERNALLY_TERMINATED".equals(state)) {
                log.info("删除了流程实例，单据号:{},原因:{}", event.getBusinessKey(), event.getDeleteReason());
            }
        }


    }

    /**
     * 监听节点事件
     * start
     */

    @EventListener
    public void handleHistoryEvent(HistoricActivityInstanceEventEntity event) {
        String eventType = event.getEventType();
        log.info("监听节点事件:{}", eventType);
        // 节点开始
        if (HistoryEventTypes.ACTIVITY_INSTANCE_START.getEventName().equals(eventType)) {
            log.info("节点开始:{}", eventType);
        }
    }


    /**
     * 开始事件
     */
    @EventListener
    public void handleProcessApplicationStartedEvent(ProcessApplicationStartedEvent event) {
        // 在这里处理ProcessApplicationStartedEvent事件
        log.info("Received ProcessApplicationStartedEvent: " + event);
    }


    /**
     * 停止事件
     */
    @EventListener
    public void handleProcessApplicationStoppedEvent(ProcessApplicationStoppedEvent event) {
        // 在这里处理ProcessApplicationStoppedEvent事件
        log.info("Received ProcessApplicationStoppedEvent: " + event);
    }

    @EventListener
    public void handleProcessApplicationEvent(ProcessApplicationEvent event) {
        // 在这里处理ProcessApplicationEvent事件
        log.info("Received ProcessApplicationEvent: " + event);
        // 可以根据需要访问事件的属性或执行其他操作
    }

}
