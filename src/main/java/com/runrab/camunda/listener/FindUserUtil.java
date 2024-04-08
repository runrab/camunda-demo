package com.runrab.camunda.listener;

import com.runrab.camunda.domain.constant.BpmRoleConstants;
import com.runrab.camunda.util.BpmModelUtil;
import com.runrab.camunda.util.BpmVariableUtil;
import jakarta.annotation.Resource;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.HistoryService;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.springframework.stereotype.Component;


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 功能描述：${findUserUtil.getUser(execution)}
 *
 * @author runrab
 * @date 2024/4/8 22:21
 */
@Slf4j
@Component("findUserUtil")
@Data
public class FindUserUtil {
    @Resource
    private RepositoryService repositoryService;
    @Resource
    private RuntimeService runtimeService;
    @Resource
    private HistoryService historyService;


    private ConcurrentHashMap<String, List<String>> concurrentHashMap = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String, Integer> countHashMap = new ConcurrentHashMap<>();


    /**
     * 分配用户
     * ${findUserUtil.getUser(execution)}
     *
     * @description 优先读取配置用户其次是默认配置的用户，
     * 如果两者均未查询到抛出异常
     */
    public List<String> getUser(DelegateExecution execution) {
        // #multiInstanceBody
        String currentActivityId = execution.getCurrentActivityId();
        String activityInstanceId = execution.getActivityInstanceId();

        // 1.一直存在的就是最新的 TODO assigneeList 新版本最好使用新的字段 转换为字符串 否则会存储在act_ge_bytearray表中
        Object variableLocal = execution.getVariable(BpmRoleConstants.ASSIGNEE_LIST);

        if (StringUtils.isNotEmpty(currentActivityId) && variableLocal != null) {
            String nodeId = BpmVariableUtil.convertNodeId(currentActivityId);
            execution.setVariable(BpmRoleConstants.ASSIGNEE_LIST + ":" + nodeId, toStrByList((List<String>) variableLocal));
        }

        // 2.判断是强制覆盖默认节点配置
        Boolean flag = (Boolean) execution.getVariable(BpmRoleConstants.COVER_USER);
        if (flag != null && flag && variableLocal != null) {
            return (List<String>) variableLocal;
        } else {
            if (currentActivityId == null) {
                // 3.会出现莫名的丢失 此时可以在不丢失前通过存储 activityInstanceId 的值，在下次丢失的时候返回这个值
                // 分配三个用户 此时需要除了第一次使用三次 使用完成后移除
                Integer i = countHashMap.get(activityInstanceId);
                if (i != null) {
                    i = i - 1;
                    if (i == 0) {
                        countHashMap.remove(activityInstanceId);
                    } else {
                        countHashMap.put(activityInstanceId, i);
                    }
                }
                return concurrentHashMap.get(activityInstanceId);
            }

            BpmnModelInstance bpmnModelInstance = repositoryService.getBpmnModelInstance(execution.getProcessDefinitionId());
            String nodeId = BpmVariableUtil.convertNodeId(currentActivityId);

            List<String> list = new ArrayList<>();
            String property = BpmModelUtil.getProperty(bpmnModelInstance, nodeId, "curSelectNodeInfo");
            if (StringUtils.isNotEmpty(property)) {
                list.addAll(getUserByProperty(property));
            }
            // 设置当前节点
            if (currentActivityId.endsWith("#multiInstanceBody")) {
                concurrentHashMap.put(activityInstanceId, list);
                countHashMap.put(activityInstanceId, list.size());
            }
            return list;
        }
    }

    public Set<String> getUserByProperty(String property) {
        Set<String> set = new HashSet<>();
        // 字符串格式 u.工号:姓名;d.部门ID:部门名称
        Arrays.asList(property.split(";")).forEach(item -> {
            if (item.startsWith("r.")) {
                // 处理角色
            } else if (item.startsWith("d.")) {
                // 处理部门
                String id = item.replace("d.", "").split(":")[0];
                set.add(id);
            } else if (item.split(":")[0].startsWith("u.")) {
                // 处理人员
                String id = item.replace("u.", "").split(":")[0];
                set.add(id);
            }
        });
        return set;
    }
    /**
     * 将list逗号分割构成String
     */

    public static final String toStrByList(List<String> list) {
        return String.join(",", list);
    }


}
