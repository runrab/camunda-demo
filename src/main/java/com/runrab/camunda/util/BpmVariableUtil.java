package com.runrab.camunda.util;

import com.runrab.camunda.domain.constant.BpmRoleConstants;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Map;

/**
 * 功能描述：Bpmn变量处理类
 *
 * @author runrab
 * @date 2024/2/7 11:39
 */
public class BpmVariableUtil {


    /**
     * 过滤节点ID中包含多实例用户任务的字符串 得到节点ID
     */
    public static String convertNodeId(String activityId) {
        if (StringUtils.isNotEmpty(activityId)) {
            return activityId.replace("#multiInstanceBody", "");
        } else {
            return "";
        }
    }

    /**
     * 过滤，处理传入变量
     */
    public Map<String, Object> initVariables(Map<String, Object> map) {
        // 1.如果未传入多实例的用户列表，就初始化一个进行占位在后期进行覆盖
        if (map.get(BpmRoleConstants.ASSIGNEE_LIST)!=null) {
            map.put(BpmRoleConstants.ASSIGNEE_LIST, new ArrayList<String>());
        }
        return map;
    }


}
