package com.runrab.camunda.service;

import org.camunda.bpm.engine.repository.ProcessDefinition;
import org.springframework.web.multipart.MultipartFile;

public interface BpmModelService {

    public ProcessDefinition deploy(MultipartFile file, String procDefName);
}
