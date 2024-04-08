package com.runrab.camunda.service.impl;

import com.runrab.camunda.service.BpmProcessService;
import jakarta.annotation.Resource;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.runtime.ProcessInstance;



/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:19
 */
public class BpmProcessServiceImpl implements BpmProcessService {
    @Resource
    private IdentityService identityService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;

    @Override
    public ProcessInstance startProcess(String startUserId, String procDefKey, String busKey) {
        // 设置用户信息 会影响到 act_hi_procinst 中的 start_user_id_
        identityService.setAuthenticatedUserId(startUserId);
        // byKey 默认发起最新的procDefId
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(procDefKey, busKey);
        return processInstance;

    }
}
