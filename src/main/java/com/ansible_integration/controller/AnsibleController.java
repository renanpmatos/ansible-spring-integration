package com.ansible_integration.controller;

import com.ansible_integration.dto.PlaybookExecutionDTO;
import com.ansible_integration.dto.PlaybookExecutionWithKeyDTO;
import com.ansible_integration.service.AnsibleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/playbooks")
public class AnsibleController {

    private final AnsibleService ansibleService;

    public AnsibleController(AnsibleService ansibleService) {
        this.ansibleService = ansibleService;
    }

    @PostMapping("/upload")
    public ResponseEntity<?> upload(@RequestParam("name") String name,
                                    @RequestParam("file") MultipartFile file) {
        try {
            if (!ansibleService.isValidYaml(file)) {
                return ResponseEntity.badRequest().body("YAML inválido");
            }

            ansibleService.savePlaybook(name, file);
            return ResponseEntity.ok("Playbook salvo com sucesso!");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao salvar o playbook.");
        }
    }

    @PostMapping("/execute")
    public ResponseEntity<?> execute(@RequestBody PlaybookExecutionWithKeyDTO dto) {
        try {
            String result = ansibleService.executePlaybook(dto.getPlaybookName(), dto.getParameters());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao executar playbook.");
        }
    }
}
