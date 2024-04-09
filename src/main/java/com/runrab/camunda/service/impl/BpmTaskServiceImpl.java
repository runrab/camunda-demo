package com.runrab.camunda.service.impl;

import com.runrab.camunda.domain.constant.BpmRoleConstants;
import com.runrab.camunda.service.BpmTaskService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.IdentityService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.history.HistoricActivityInstance;
import org.camunda.bpm.engine.history.HistoricTaskInstance;
import org.camunda.bpm.engine.runtime.ActivityInstance;
import org.camunda.bpm.engine.task.Task;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:17
 */
@Service
@Slf4j
public class BpmTaskServiceImpl implements BpmTaskService {
    @Resource
    private TaskService taskService;
    @Resource
    private IdentityService identityService;

    @Resource
    private HistoryService historyService;
    @Resource
    private RuntimeService runtimeService;
    @Override
    public void approveTask(String taskId){
        taskService.complete(taskId);
    }



    /**
     * 驳回
     * */
    @Override
    public void rejectTask(String taskId,String userId,String comment) {
//        identityService.setAuthenticatedUserId(userId);

        Task task = taskService.createTaskQuery().taskId(taskId).singleResult();
        String procInstId=task.getProcessInstanceId();

        ActivityInstance tree = runtimeService.getActivityInstance(procInstId);
        //获取所有已办用户任务节点
        List<HistoricActivityInstance> resultList = historyService
                .createHistoricActivityInstanceQuery()
                .processInstanceId(procInstId)
                .activityType("userTask")
                .finished()
                .orderByHistoricActivityInstanceEndTime()
                .asc()
                .list();
        if (null == resultList || resultList.isEmpty()) {
            throw new RuntimeException("当前任务无法驳回！");
        }
        //得到第一个任务节点的id
        HistoricActivityInstance historicActivityInstance = resultList.get(0);
        String startActId = historicActivityInstance.getActivityId();
        if (startActId.equals(task.getTaskDefinitionKey())) {
            throw new RuntimeException("开始节点无法驳回！");
        }
        //得到上一个任务节点的ActivityId和待办人
        Map<String, String> lastNode = getLastNode(resultList, task.getTaskDefinitionKey());
        if (null == lastNode) {
            throw new RuntimeException("回退节点异常！");
        }
        try {
            String toActId = lastNode.get("toActId");
            String assignee = lastNode.get(BpmRoleConstants.ASSIGNEE);
            //设置流程中的可变参数
            Map<String, Object> taskVariable = new HashMap<>();
            taskVariable.put(BpmRoleConstants.ASSIGNEE, assignee);
            // 驳回原因
            taskService.createComment(taskId, procInstId, "驳回:" + comment);
            runtimeService.createProcessInstanceModification(procInstId)
                    .cancelActivityInstance(getInstanceIdForActivity(tree, task.getTaskDefinitionKey()))//关闭相关任务
                    .setAnnotation("进行了驳回到上一个任务节点操作")
                    .startBeforeActivity(toActId)//启动目标活动节点
                    .setVariables(taskVariable)//流程的可变参数赋值
                    .execute();
        } catch (Exception e) {
            log.error("驳回失败！" + e.getMessage());
            throw new RuntimeException("驳回失败！" + e.getMessage());
        }
    }


    /**
     * 获取上一节点信息
     * 分两种情况：
     * 1、当前节点不在历史节点里
     * 2、当前节点在历史节点里
     * 比如，resultList={1,2,3}
     * (1)当前节点是4，表示3是完成节点，4驳回需要回退到3
     * (2)当前节点是2，表示3是驳回节点，3驳回到当前2节点，2驳回需要回退到1
     * 其他驳回过的情况也都包含在情况2中。
     *
     * @param resultList        历史节点列表
     * @param currentActivityId 当前待办节点ActivityId
     * @return 返回值：上一节点的ActivityId和待办人（toActId, assignee）
     */
    private static Map<String, String> getLastNode(List<HistoricActivityInstance> resultList, String currentActivityId) {
        Map<String, String> backNode = new HashMap<>();
        LinkedHashMap<String, String> linkedHashMap = new LinkedHashMap();
        for (HistoricActivityInstance hai : resultList) {
            linkedHashMap.put(hai.getActivityId(), hai.getAssignee());
        }
        int originSize = resultList.size();
        boolean flag = false;
        for (Map.Entry entry : linkedHashMap.entrySet()) {
            if (currentActivityId.equals(entry.getKey())) {
                flag = true;
                break;
            }
        }
        if (!flag) {
            HistoricActivityInstance historicActivityInstance = resultList.get(originSize - 1);
            backNode.put("toActId", historicActivityInstance.getActivityId());
            backNode.put(BpmRoleConstants.ASSIGNEE, historicActivityInstance.getAssignee());
            return backNode;
        }
        return currentNodeInHis(linkedHashMap, currentActivityId);
    }
    private static Map<String, String> currentNodeInHis(LinkedHashMap<String, String> linkedHashMap, String currentActivityId) {
        Map<String, String> backNode = new HashMap<>();
        ListIterator<Map.Entry<String, String>> li = new ArrayList<>(linkedHashMap.entrySet()).listIterator();
        while (li.hasNext()) {
            Map.Entry<String, String> entry = li.next();
            if (currentActivityId.equals(entry.getKey())) {
                li.previous();
                Map.Entry<String, String> previousEntry = li.previous();
                backNode.put("toActId", previousEntry.getKey());
                backNode.put(BpmRoleConstants.ASSIGNEE, previousEntry.getValue());
                return backNode;
            }
        }
        return null;
    }

    private String getInstanceIdForActivity(ActivityInstance activityInstance, String activityId) {
        ActivityInstance instance = getChildInstanceForActivity(activityInstance, activityId);
        return instance != null ? instance.getId() : null;
    }

    private ActivityInstance getChildInstanceForActivity(ActivityInstance activityInstance, String activityId) {
        if (activityId.equals(activityInstance.getActivityId())) {
            return activityInstance;
        }
        for (ActivityInstance childInstance : activityInstance.getChildActivityInstances()) {
            ActivityInstance instance = getChildInstanceForActivity(childInstance, activityId);
            if (instance != null) {
                return instance;
            }
        }
        return null;
    }

    @Override
    public void copyTask(String taskId, List<String> userList) {
        try {
            HistoricTaskInstance instance = historyService.createHistoricTaskInstanceQuery().taskId(taskId).singleResult();
            String name = instance.getName();
            String procInstId = instance.getProcessInstanceId();
            for (String userId : userList) {
                Task task = taskService.newTask();
                task.setAssignee(userId);
                task.setDescription("抄送任务");
                task.setName(name);
                task.setOwner(userId);
                // 不可设置父任务ID 否则会抄送任务会影响流程的正常结束
//                task.setParentTaskId(taskId);
                task.setCaseInstanceId(procInstId);
                taskService.saveTask(task);
            }
            // TODO 批量更新 act_hi_comment表procInstId
        } catch (RuntimeException e) {
            log.error("任务抄送失败:" + e.getMessage());
            throw new RuntimeException("任务抄送失败:" + e.getMessage());
        }
    }


    /**
     * 主动撤回给自己
     * */
    @Override
    public void cancelTask(String taskId) {

    }

    /**
     * 委派/借阅给指定用户
     * 会返还给自己
     */
    @Override
    public void delegateTask(String taskId, String userId) {
        try {
            taskService.delegateTask(taskId, userId);
        } catch (RuntimeException e) {
            log.error("委托任务失败:{}", e.getMessage());
            throw new RuntimeException("委托任务失败:" + e.getMessage());
        }
    }

    /**
     * 转办
     *
     * @param taskId 任务id
     * @param userId 目标人
     */
    @Override
    public void turnTask(String taskId, String userId) {
        try {
            taskService.setAssignee(taskId, userId);
        } catch (RuntimeException e) {
            log.error("任务转办失败:" + e.getMessage());
            throw new RuntimeException("任务转办失败:" + e.getMessage());
        }
    }




    /**
     * 我的代办 我的已办 我的任务
     * */
    @Override
    public List<HistoricTaskInstance> getUserTask(String userId,String procInstId){
        // 代办
       historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).unfinished().list();
       // 已办
        historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).finished().list();
        // 抄送
        historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).caseInstanceId(procInstId).list();
        // 除了抄送任务外的
       historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).processInstanceId(procInstId).list();

     return  historyService.createHistoricTaskInstanceQuery().taskAssignee(userId).list();

    }



}
