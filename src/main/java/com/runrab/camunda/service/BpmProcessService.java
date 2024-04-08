package com.runrab.camunda.service;


import org.camunda.bpm.engine.runtime.ProcessInstance;

public interface BpmProcessService {

    public ProcessInstance startProcess(String startUserId, String procDefKey, String busKey);
}
