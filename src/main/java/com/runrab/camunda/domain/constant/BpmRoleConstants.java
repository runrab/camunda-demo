package com.runrab.camunda.domain.constant;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2023/8/28 13:30
 */
public class BpmRoleConstants {

    /**
     * 流程发起人
     */
    public static final String START_USER_ID = "startUserId";
    /**
     * 当前节点分配人
     */
    public static final String ASSIGNEE = "assignee";
    /**
     * 当前节点分配人列表
     */
    public static final String ASSIGNEE_LIST = "assigneeList";

    /**
     * 强制覆盖预选用户 默认false
     */
    public static final String COVER_USER = "coverUser";

    public static final String ADD_USER_IDS = "addUserIds";


}
