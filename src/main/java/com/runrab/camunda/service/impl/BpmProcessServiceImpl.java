package com.runrab.camunda.service.impl;

import com.runrab.camunda.service.BpmProcessService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.*;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.bpm.engine.runtime.ProcessInstanceModificationBuilder;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.ExtensionElements;
import org.camunda.bpm.model.bpmn.instance.FlowNode;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.jvnet.hk2.annotations.Service;

import java.util.*;


/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:19
 */
@Service
@Slf4j
public class BpmProcessServiceImpl implements BpmProcessService {
    @Resource
    private IdentityService identityService;
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;

    @Resource
    private TaskService taskService;
    @Override
    public ProcessInstance startProcess(String startUserId, String procDefKey, String busKey) {
        // 设置用户信息 会影响到 act_hi_procinst 中的 start_user_id_
        identityService.setAuthenticatedUserId(startUserId);
        // byKey 默认发起最新的procDefId
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(procDefKey, busKey);
        return processInstance;

    }


    public void jumpNode(String procInstId,String nodeId,String userId,Map<String,Object> oldParams){

        try {
            ProcessInstanceModificationBuilder builder = runtimeService.createProcessInstanceModification(procInstId);
            Set<String> activityIdSet = new HashSet<>();
            taskService.createTaskQuery().processInstanceId(procInstId).active().list().forEach(taskQuery -> {
                String activityId = taskQuery.getTaskDefinitionKey();
                if (activityIdSet.add(activityId)) {
                    builder.cancelAllForActivity(activityId);
                }
            });
            //获得需要分配的用户变量
            String str = getAssigneeList(procInstId, nodeId);
            // 还原变量 因为选择用户的监听器前置 固需要连线处的变量 TODO
            Map<String, Object> originalVariables = runtimeService.getVariables(procInstId);
            // 手动刷新目标节点的用户
            originalVariables.put("assigneeList", findUserList(str));
            if (oldParams != null) {
                originalVariables.putAll(oldParams);
            }
            builder.startBeforeActivity(nodeId + "#multiInstanceBody")// 因为是多实例需要添加后缀 如果不是多实例节点不需要可以先校验当前节点
                    .setVariables(originalVariables)
                    .setAnnotation( userId+ "操作了跳转了活动实例")
                    .execute();
            // TODO 增加审批意见
        } catch (RuntimeException e) {
            log.error("跳转到指定流程节点失败 " + e.getMessage());
            throw new RuntimeException("跳转到指定流程节点失败:" + e.getMessage());
        }
    }
    public String getAssigneeList(String procInstId, String actId) {
        String procDefId = runtimeService.createProcessInstanceQuery().processInstanceId(procInstId).singleResult().getProcessDefinitionId();
        List<CamundaProperties> list = getExtension(procDefId, actId).getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list();
        for (CamundaProperties properties : list) {
            Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
            for (CamundaProperty property : camundaProperties) {
                // curNodeSelect 代表前端扩展属性中的用户信息存储字段
                if ("curNodeSelect".equals(property.getCamundaName())) {
                    return property.getCamundaValue();
                }
            }
        }
        return "";
    }


    /**
     * 获得当前节点扩展属性对象
     */
    public ExtensionElements getExtension(String procDefId, String actId) {
        BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(procDefId);
        FlowNode element = bpmnModelInstance.getModelElementById(actId);
        return element.getExtensionElements();
    }
    /**
     * 查询用户
     */
    public Set<String> findUserList(String expression) {
        Set<String> set = new HashSet<>();
        //  模拟用户查找逻辑
        set.add("admin");

        return set;
    }
}
