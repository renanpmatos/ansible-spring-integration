package com.ansible_integration.dto;

import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

public class PlaybookExecutionWithKeyDTO {

    private String playbookName;
    private Map<String, String> parameters;
    private MultipartFile privateKey;

    public String getPlaybookName() {
        return playbookName;
    }

    public void setPlaybookName(String playbookName) {
        this.playbookName = playbookName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, String> parameters) {
        this.parameters = parameters;
    }

    public MultipartFile getPrivateKey() {
        return privateKey;
    }

    public void setPrivateKey(MultipartFile privateKey) {
        this.privateKey = privateKey;
    }
}
