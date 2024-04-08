package com.runrab.camunda.util;


import com.runrab.camunda.domain.dto.NextNodeInfo;
import org.apache.commons.lang3.StringUtils;
import org.camunda.bpm.engine.delegate.TaskListener;
import org.camunda.bpm.engine.impl.core.model.Properties;
import org.camunda.bpm.engine.impl.pvm.process.ActivityImpl;
import org.camunda.bpm.model.bpmn.BpmnModelInstance;
import org.camunda.bpm.model.bpmn.instance.*;
import org.camunda.bpm.model.bpmn.instance.Process;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperties;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaProperty;
import org.camunda.bpm.model.bpmn.instance.camunda.CamundaTaskListener;
import org.camunda.bpm.model.xml.instance.ModelElementInstance;
import org.camunda.bpm.model.xml.type.ModelElementType;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 功能描述：流程定义模型操作工具类
 *
 * @author runrab
 * @date 2023/12/6 9:44
 */
public class BpmModelUtil {

    /*
     * 获取下一步用户任务的节点ID
     * @param node 当前节点
     * @param bpmnModelInstance 流程实例
     */
    public static Map<String, String> getNextIds(BpmnModelInstance bpmnModelInstance, FlowNode node) {
        Set<String> stringSet = new HashSet<>();
        Map<String, String> map = new HashMap<>();
        for (SequenceFlow flow : node.getOutgoing()) {
            FlowNode target = flow.getTarget();
            if (isUserTask(target)) {
                boolean add = stringSet.add(target.getId());
                if (add) {
                    map.put(target.getId(), target.getName());
                }
            } else if (isGateway(target)) {
                // 嵌套循环
//                stringSet.addAll(getNextIds(bpmnModelInstance, target.getId(), target));
                map.putAll(getNextIds(bpmnModelInstance, target));
            } else {
                // 结束节点直接返回
                break;
            }
        }
        return map;
    }


    /**
     * 当前节点是否为用户任务节点
     *
     * @param node 当前节点元素
     * @return boolean
     */
    public static boolean isUserTask(FlowNode node) {
        return "userTask".equals(node.getElementType().getTypeName()) || "multiInstanceBody".equals(node.getElementType().getTypeName());
    }


    /**
     * 当前节点是否为多用户任务节点
     *
     * @param nodeId 当前节点元素Id
     * @return boolean
     */
    public static boolean isMulUserTask(BpmnModelInstance bpmnModelInstance, String nodeId) {
        try {
            UserTask node = bpmnModelInstance.getModelElementById(nodeId);
            LoopCharacteristics loopCharacteristics = node.getLoopCharacteristics();
            return loopCharacteristics != null;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 校验流程图前方节点是否涉及并行网关
     * 从当前节点一直查找前方节点
     *
     * @param nodeId 必须是用户任务的节点
     */
    public static boolean hasPreParallelGateway(BpmnModelInstance bpmnModelInstance, String nodeId) {

        Collection<ParallelGateway> list = bpmnModelInstance.getModelElementsByType(ParallelGateway.class);

        boolean exits = false;
        if (!list.isEmpty()) {
            exits = true;
        } else {
            FlowNode flowNode = bpmnModelInstance.getModelElementById(nodeId);
            Collection<SequenceFlow> incoming = flowNode.getIncoming();
            // 不需要判断是从那条线流入的 只需要判断是否存在就行
            for (SequenceFlow sequenceFlow : incoming) {
                FlowNode source = sequenceFlow.getSource();
                if ("parallelGateway".equals(source.getElementType().getTypeName())) {
                    exits = true;
                }
            }
        }
        return exits;
    }

    /**
     * 校验流程图是否存在指定类型的节点
     */
    public static <T> boolean hasOneNode(BpmnModelInstance bpmnModelInstance, ModelElementType referencingType) {

        Collection<ModelElementInstance> modelElementsByType = bpmnModelInstance.getModelElementsByType(referencingType);

        return !modelElementsByType.isEmpty();
    }

    /**
     * 校验流程图是否存在并行网关类型的节点
     * 示例: parallelGateway
     * ParallelGateway.class
     */
    public static <T> boolean hasParallelGateway(BpmnModelInstance bpmnModelInstance) {

        Collection<ParallelGateway> modelElementsByType = bpmnModelInstance.getModelElementsByType(ParallelGateway.class);

        return !modelElementsByType.isEmpty();
    }

    /**
     * 当前节点是否为网关
     *
     * @param node 当前节点元素
     * @description 判断是否为 并行网关 单一网关 和包含网关
     */
    private static boolean isGateway(FlowNode node) {
        String typeName = node.getElementType().getTypeName();
        return "parallelGateway".equals(typeName) || "exclusiveGateway".equals(typeName) || "inclusiveGateway".equals(typeName);
    }

    /**
     * 获得流程图节点排序
     *
     * @param bpmnModelInstance 流程模型实例
     */
    //   3.开始===========================
    public static LinkedList<Map<String, String>> getSortedNodes(BpmnModelInstance bpmnModelInstance) {
        if (ObjectUtils.isEmpty(bpmnModelInstance)) {
            return null;
        }
        LinkedList<Map<String, String>> nodeList = new LinkedList<>();
//        Collection<FlowNode> flowNodes = bpmnModelInstance.getModelElementsByType(FlowNode.class);
        // 构建有向图
        Map<String, List<String>> graph = buildGraph(bpmnModelInstance);
        // 进行拓扑排序
        List<String> sortedNodeIds = sortNode(graph);
        // 将排序后的节点信息添加到列表中
        sortedNodeIds.forEach(nodeId -> {
            FlowNode node = bpmnModelInstance.getModelElementById(nodeId);
            if (node != null) {
                Map<String, String> nodeInfo = new HashMap<>();
                nodeInfo.put("nodeId", node.getId());
                nodeInfo.put("nodeName", node.getName());
                nodeInfo.put("nodeType", node.getElementType().getTypeName());
                nodeList.add(nodeInfo);
            }
        });
        return nodeList;
    }

    // 构建有向图
    private static Map<String, List<String>> buildGraph(BpmnModelInstance bpmnModelInstance) {
        Map<String, List<String>> graph = new HashMap<>();
        Collection<SequenceFlow> sequenceFlows = bpmnModelInstance.getModelElementsByType(SequenceFlow.class);

        for (SequenceFlow flow : sequenceFlows) {
            String sourceNodeId = flow.getSource().getId();
            String targetNodeId = flow.getTarget().getId();
            graph.computeIfAbsent(sourceNodeId, k -> new ArrayList<>()).add(targetNodeId);
        }

        // 如果有开始事件，将开始事件作为一个虚拟的源节点，方便拓扑排序
        StartEvent startEvent = bpmnModelInstance.getModelElementsByType(StartEvent.class).stream().findFirst().orElse(null);
        if (startEvent != null) {
            graph.computeIfAbsent("StartEvent", k -> new ArrayList<>()).add(startEvent.getId());
        }
        return graph;
    }

    // 进行拓扑排序
    private static List<String> sortNode(Map<String, List<String>> graph) {
        List<String> sortedNodes = new ArrayList<>();
        Set<String> visited = new HashSet<>();
        for (String nodeId : graph.keySet()) {
            if (!visited.contains(nodeId)) {
                nodeSortUtil(nodeId, graph, visited, sortedNodes);
            }
        }
        Collections.reverse(sortedNodes);
        return sortedNodes;
    }

    private static void nodeSortUtil(String nodeId, Map<String, List<String>> graph, Set<String> visited, List<String> sortedNodes) {
        visited.add(nodeId);
        List<String> neighbors = graph.getOrDefault(nodeId, Collections.emptyList());
        for (String neighbor : neighbors) {
            if (!visited.contains(neighbor)) {
                nodeSortUtil(neighbor, graph, visited, sortedNodes);
            }
        }
        sortedNodes.add(nodeId);
    }

    /**
     * 查询扩展属性
     */
    public static Map<String, Object> getProperties(BpmnModelInstance bpmnModelInstance, String nodeId) {
        Map<String, Object> map = new HashMap<>();
        ExtensionElements e = BpmModelUtil.getExtensionElements(bpmnModelInstance, nodeId);
        if (null == e) {
            return map;
        }
        // 根据类型查询
        List<CamundaProperties> list = e.getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list();
        // 遍历属性封装Map
        for (CamundaProperties properties : list) {
            Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
            camundaProperties.forEach(v -> {
                map.put(v.getCamundaName(), v.getCamundaValue());
            });
        }
        return map;
    }

    /**
     * 获取节点扩展属性
     */
    public static String getProperty(BpmnModelInstance bpmnModelInstance, String nodeId, String key) {
        String str = null;
        ExtensionElements e = BpmModelUtil.getExtensionElements(bpmnModelInstance, nodeId);
        if (null == e) {
            return str;
        }
        // 根据类型查询
        List<CamundaProperties> list = e.getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list();
        // 遍历属性封装Map
        for (CamundaProperties properties : list) {
            Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
            // 一个属性值获取一个 不能重复
            List<CamundaProperty> propertyList = camundaProperties.stream().filter(property -> property.getCamundaName().equals(key)).collect(Collectors.toList());
            if (!propertyList.isEmpty()) {
                str = propertyList.get(0).getCamundaValue();
            }
        }
        return str;
    }


    //获得当前节点扩展属性对象
    public static ExtensionElements getExtensionElements(BpmnModelInstance bpmnModelInstance, String nodeId) {
        FlowNode element = bpmnModelInstance.getModelElementById(nodeId);
        return element != null ? element.getExtensionElements() : null;
    }

    // 3.======排序结束================================

    // TODO 使用 getGlobalProperties 替换
    public static Map<String, Object> getRootProperties(BpmnModelInstance bpmnModelInstance) {
        Map<String, Object> map = new HashMap<>();
        ExtensionElements e = getRootExtensionElements(bpmnModelInstance);
        if (null == e) {
            return map;
        }
        // 根据类型查询
        List<CamundaProperties> list = e.getElementsQuery()
                .filterByType(CamundaProperties.class)
                .list();
        // 遍历属性封装Map
        for (CamundaProperties properties : list) {
            Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
            camundaProperties.forEach(v -> {
                map.put(v.getCamundaName(), v.getCamundaValue());
            });
        }
        return map;
    }

    // 获得流程图根节点的扩展属性对象
    public static ExtensionElements getRootExtensionElements(BpmnModelInstance bpmnModelInstance) {
        Collection<org.camunda.bpm.model.bpmn.instance.Process> processes = bpmnModelInstance.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class);
        if (!processes.isEmpty()) {
            org.camunda.bpm.model.bpmn.instance.Process process = processes.iterator().next();
            return process.getExtensionElements();
        }
        return null;
    }

    /**
     * 获得第一个用户任务节点
     *
     * @param bpmnModelInstance 流程定义模型
     * @return firstNodeId  开始节点后紧跟的用户节点的nodeId
     */
    public static String getFirstNode(BpmnModelInstance bpmnModelInstance) {
        String firstNodeId = "";
        // 1.获得开始节点
        StartEvent startEvent = bpmnModelInstance.getModelElementsByType(StartEvent.class).stream().findFirst().orElse(null);
        // 2.获得开始节点后的连线
        if (startEvent != null) {
            Collection<SequenceFlow> outgoing = startEvent.getOutgoing();
            //2.1 当连线只有一条
            if (outgoing.size() == 1) {
                // 3. 如果连线只有一个且 紧接着是用户任务节点  如果:是单用户节点默认是系统提供的申请人 节点
                for (SequenceFlow sequenceFlow : outgoing) {
                    FlowNode target = sequenceFlow.getTarget();
                    String typeName = target.getElementType().getTypeName();
                    if ("userTask".equals(typeName)) {
                        return target.getId();
                    }
                }
            }
        }
        return firstNodeId;
    }

    /**
     * 获得流入线元素
     *
     * @return 连线列表
     */
    public static List<SequenceFlow> getIncomingLine(BpmnModelInstance bpmnModelInstance, String nodeId) {
        FlowNode element = bpmnModelInstance.getModelElementById(nodeId);
        return new ArrayList<>(element.getIncoming());
    }

    /**
     * 获取当前节点后续节点
     * 忽略所有网关
     */
    public static List<NextNodeInfo> getNextNode(FlowNode node) {

        Collection<SequenceFlow> outgoing = node.getOutgoing();
        List<NextNodeInfo> nextNodes = new ArrayList<>();
        for (SequenceFlow sequenceFlow : outgoing) {
            FlowNode target = sequenceFlow.getTarget();
            // 1. 设置节点属性
            String typeName = target.getElementType().getTypeName();
            NextNodeInfo nextNodeInfo = new NextNodeInfo();
            nextNodeInfo.setNodeId(target.getId());
            nextNodeInfo.setNodeName(target.getName());
            nextNodeInfo.setTypeName(typeName);

            // 2. 条件表达式
            ConditionExpression condition = sequenceFlow.getConditionExpression();

            // 如果当前节点是网关下一个节点必定不能为网关
            if (condition != null) {
                // 获取 sequenceFlow 上的条件配置  只有脚本/字符串 这里只使用字符串
                String rawTextContent = condition.getRawTextContent();
                if (StringUtils.isNotEmpty(rawTextContent)) {
                    nextNodeInfo.setSkip(rawTextContent);
                }
            }
            // 3.封装目标节点扩展属性
            ExtensionElements elements = target.getExtensionElements();
            if (elements != null) {
                Map<String, Object> map = new HashMap<>();
                // 根据类型查询
                List<CamundaProperties> list = elements.getElementsQuery()
                        .filterByType(CamundaProperties.class)
                        .list();
                // 遍历属性封装Map
                for (CamundaProperties properties : list) {
                    Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
                    camundaProperties.forEach(v -> {
                        map.put(v.getCamundaName(), v.getCamundaValue());
                    });
                }
                nextNodeInfo.setProperties(map);
            }
            //4.判断当前节点类型
            if (typeName.endsWith("Gateway")) {
                List<NextNodeInfo> gatewayNextNodes = getNextNode(target);
                nextNodes.addAll(gatewayNextNodes);
            } else {
                // 处理非网关  任务(用户任务/服务任务等) 结束事件
                nextNodes.add(nextNodeInfo);
            }
        }
        return nextNodes;
    }
    //  ===================获得流程图节点排序-结束 ==================

    /**
     * 获取整个流程构成的树，条件表达式存储在连线的目标节点
     * 忽略所有网关
     */
    public static TreeMap<String, NextNodeInfo> getProcessTree(FlowNode node) {
        TreeMap<String, NextNodeInfo> treeMap = new TreeMap<>();

        // 将开始节点添加到树中
        NextNodeInfo startNodeInfo = new NextNodeInfo();
        startNodeInfo.setNodeId(node.getId());
        startNodeInfo.setNodeName(node.getName());
        startNodeInfo.setTypeName(node.getElementType().getTypeName());

        treeMap.put(node.getId(), startNodeInfo);

        // 递归构建整个流程树
        buildProcessTree(node, treeMap);

        return treeMap;
    }

    // 递归构建流程树
    private static void buildProcessTree(FlowNode node, TreeMap<String, NextNodeInfo> treeMap) {
        Collection<SequenceFlow> outgoing = node.getOutgoing();
        for (SequenceFlow sequenceFlow : outgoing) {
            FlowNode target = sequenceFlow.getTarget();
            String typeName = target.getElementType().getTypeName();

            // 如果是网关，不将其加入树中，继续递归查找下一个节点
            if (typeName.endsWith("Gateway")) {
                buildProcessTree(target, treeMap);
                continue;
            }

            // 设置节点属性
            NextNodeInfo nextNodeInfo = new NextNodeInfo();
            nextNodeInfo.setNodeId(target.getId());
            nextNodeInfo.setNodeName(target.getName());
            nextNodeInfo.setTypeName(typeName);

            // 获取条件表达式
            ConditionExpression condition = sequenceFlow.getConditionExpression();
            if (condition != null) {
                String rawTextContent = condition.getRawTextContent();
                if (rawTextContent != null && !rawTextContent.isEmpty()) {
                    nextNodeInfo.setSkip(rawTextContent);
                }
            }

            // 封装目标节点扩展属性
            ExtensionElements elements = target.getExtensionElements();
            if (elements != null) {
                Map<String, Object> map = new HashMap<>();
                List<CamundaProperties> list = elements.getElementsQuery()
                        .filterByType(CamundaProperties.class)
                        .list();
                for (CamundaProperties properties : list) {
                    Collection<CamundaProperty> camundaProperties = properties.getCamundaProperties();
                    camundaProperties.forEach(v -> {
                        map.put(v.getCamundaName(), v.getCamundaValue());
                    });
                }
                nextNodeInfo.setProperties(map);
            }

            treeMap.put(target.getId(), nextNodeInfo);
        }
    }

    public static Map<String, Object> getGlobalProperties(BpmnModelInstance bpmnModelInstance) {
        Map<String, Object> map = new HashMap<>();
        Collection<org.camunda.bpm.model.bpmn.instance.Process> processCollection = bpmnModelInstance.getModelElementsByType(org.camunda.bpm.model.bpmn.instance.Process.class);
        for (Process process : processCollection) {
            ExtensionElements extensionElements = process.getExtensionElements();
            Collection<CamundaProperty> properties = extensionElements.getElementsQuery()
                    .filterByType(CamundaProperties.class)
                    .singleResult()
                    .getCamundaProperties();
            for (CamundaProperty property : properties) {
                String name = property.getCamundaName();
                String value = property.getCamundaValue();
                map.put(name, value);
            }
        }
        return map;
    }

    /**
     * TODO 为模型增加任务监听器 不建议使用
     */
    @Deprecated
    public static BpmnModelInstance addTaskListener(BpmnModelInstance bpmnModelInstance) {
        Collection<Task> userTasks = bpmnModelInstance.getModelElementsByType(Task.class);
        for (Task userTask : userTasks) {
            // 创建并添加任务监听器
            for (String eventName : Arrays.asList(TaskListener.EVENTNAME_ASSIGNMENT, TaskListener.EVENTNAME_COMPLETE, TaskListener.EVENTNAME_DELETE)) {
                CamundaTaskListener taskListener = bpmnModelInstance.newInstance(CamundaTaskListener.class);
                taskListener.setCamundaDelegateExpression("${pushOaMsg}");
                taskListener.setCamundaEvent(eventName);
                userTask.builder().addExtensionElement(taskListener);
            }
        }
        return bpmnModelInstance;
    }

    /**
     * 指示活动是否为多实例活动。
     * <p>
     * 如果此活动是多实例活动，则 @return true。
     */
    public boolean isMultiInstance(ActivityImpl activity) {
        return activity.isMultiInstance();
    }

    /**
     * @param activity 当前节点
     * @param key      key值
     */
    public Object getProperty(ActivityImpl activity, String key) {
        return activity.getProperty(key);
    }

    /**
     * @param activity 当前节点
     * @return Properties  Map类型
     */
    public Properties getProperties(ActivityImpl activity) {
        return activity.getProperties();
    }

    /**
     * 获得流入线的ID
     *
     * @param bpmnModelInstance bpmn实例
     * @return List<String>
     * @description
     */
    public List<String> getIncomingLineId(BpmnModelInstance bpmnModelInstance, String nodeId) {
        List<String> lines = new ArrayList<>();
        FlowNode element = bpmnModelInstance.getModelElementById(nodeId);
        //入线
        Collection<SequenceFlow> sequenceFlow = element.getIncoming();
        sequenceFlow.forEach(flowNode -> {
            lines.add(flowNode.getId());
        });
        return lines;
    }

    /**
     * 返回所有用户任务节点-无序
     *
     * @return List
     * @description nodeId 节点Id nodeName 节点名称  nodeType 节点类型
     */
    public List<Map<String, String>> getUserNodeInfo(BpmnModelInstance bpmnModelInstance) {
        Collection<UserTask> element = bpmnModelInstance.getModelElementsByType(UserTask.class);
        LinkedList<Map<String, String>> list = new LinkedList<>();
        element.forEach(userTask -> {
            Map<String, String> map = new HashMap<>();
            map.put("nodeId", userTask.getId());
            map.put("nodeName", userTask.getName());
            map.put("nodeType", userTask.getElementType().getTypeName());
            list.add(map);
        });
        return list;
    }


}
