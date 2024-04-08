package com.runrab.camunda.listener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateTask;
import org.camunda.bpm.engine.delegate.Expression;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 功能描述：任务监听器示例
 *
 * @author runrab
 * @date 2024/4/8 22:24
 */
@Component("taskListenerDemo")
@Slf4j
@Data
public class TaskListenerDemo implements TaskListener  {

    // 对应监听器中的字段
    private Expression expression;

    @Override
    public void notify(DelegateTask delegateTask) {
        String asExprValue = (String) expression.getValue(delegateTask);

        String eventName = delegateTask.getEventName();

        //监听器顺序 create->assignee 类型如下
        switch (eventName) {
            case TaskListener.EVENTNAME_CREATE:
                log.info(TaskListener.EVENTNAME_CREATE);
            case TaskListener.EVENTNAME_ASSIGNMENT:
                log.info(TaskListener.EVENTNAME_ASSIGNMENT);
            case TaskListener.EVENTNAME_COMPLETE:
                log.info(TaskListener.EVENTNAME_COMPLETE);
            case TaskListener.EVENTNAME_UPDATE:
                log.info(TaskListener.EVENTNAME_UPDATE);
            case TaskListener.EVENTNAME_DELETE:
                log.info(TaskListener.EVENTNAME_DELETE);
            case TaskListener.EVENTNAME_TIMEOUT:
                log.info(TaskListener.EVENTNAME_TIMEOUT);
            default:
                break;
        }

        // 前端流程图节点的ID
        String taskDefinitionKey = delegateTask.getTaskDefinitionKey();
        // 推荐设置变量的时候 增加个节点ID 用于防止被覆盖
        delegateTask.setVariable("variable","variable");
        delegateTask.setVariableLocal("variableLocal","variableLocal");


    }

}
