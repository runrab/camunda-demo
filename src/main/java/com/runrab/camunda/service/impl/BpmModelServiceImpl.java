package com.runrab.camunda.service.impl;

import com.runrab.camunda.service.BpmModelService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RepositoryService;
import org.camunda.bpm.engine.repository.Deployment;
import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;


import java.io.IOException;
import java.io.InputStream;

/**
 * 功能描述：
 *
 * @author runrab
 * @date 2024/4/8 22:18
 */
@Service
@Slf4j
public class BpmModelServiceImpl implements BpmModelService {

    @Resource
    private RepositoryService repositoryService;


    /**
     * 建议部署xml文件的同时部署svg文件这样的得到的流程图更清晰
     * */
    @Override
    public ProcessDefinition deploy(MultipartFile file, String procDefName) {
        try {
            InputStream inputStream = file.getInputStream();
            Deployment deployment = repositoryService.createDeployment().name(procDefName)
                    .addInputStream(procDefName + ".bpmn", inputStream).deploy();//完成部署
            ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery().deploymentId(deployment.getId()).singleResult();

            return  processDefinition;
        } catch (RuntimeException | IOException e) {
            log.error("部署失败!: {}", e.getMessage());
            throw new RuntimeException("部署失败! " + e.getMessage());
        }
    }
}
