package com.runrab.camunda.listener;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:23
 */
@Data
@Slf4j
@Component("javaDelegateDemo")
public class JavaDelegateDemo implements JavaDelegate {
    @Override
    public void execute(DelegateExecution execution) throws Exception {

    }
}
