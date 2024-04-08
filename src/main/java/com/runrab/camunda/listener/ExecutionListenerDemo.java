package com.runrab.camunda.listener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.ExecutionListener;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.springframework.stereotype.Component;

/**
 * 功能描述：执行监听器
 *
 * @author runrab
 * @date 2024/4/8 22:26
 */
@Slf4j
@Data
@Component("executionListenerDemo")
public class ExecutionListenerDemo implements ExecutionListener {
    @Override
    public void notify(DelegateExecution execution) throws Exception {

        String eventName = execution.getEventName();
        // 顺序 start->take->end
        switch (eventName) {
            case ExecutionListener.EVENTNAME_START:
                log.info(ExecutionListener.EVENTNAME_START);
            case ExecutionListener.EVENTNAME_TAKE:
                log.info(ExecutionListener.EVENTNAME_TAKE);
            case ExecutionListener.EVENTNAME_END:
                log.info(ExecutionListener.EVENTNAME_END);
            default:
                break;
        }
    }
}
