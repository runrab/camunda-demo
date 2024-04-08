package com.runrab.camunda.domain.dto;

import lombok.Data;

import java.util.Map;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:43
 */
@Data
public class NextNodeInfo {

    private String nodeId;
    private String nodeName;
    /**
     * 节点类型名称
     */
    private String typeName;
    //    private boolean type;//是否互斥 只有单一网关是互斥的 任务节点和并行任务节点都不是互斥的
    private String skip; // 跳转条件 只有单一网关存在的时候存在
    /**
     * 节点扩展属性
     */
    private Map<String, Object> properties;
}
