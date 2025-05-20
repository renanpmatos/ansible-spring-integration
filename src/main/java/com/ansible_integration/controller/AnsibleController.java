package com.ansible_integration.controller;

import com.ansible_integration.dto.PlaybookExecutionWithKeyDTO;
import com.ansible_integration.service.AnsibleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

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

    @PostMapping(value = "/execute", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> executePipeline(
            @RequestParam("playbook") MultipartFile playbookFile,
            @RequestParam("privateKey") MultipartFile privateKeyFile,
            @RequestParam Map<String, String> allRequestParams
    ) {
        try {
            // Remove os arquivos específicos dos parametros
            allRequestParams.remove("playbook");
            allRequestParams.remove("privateKey");

            String output = ansibleService.executePlaybook(playbookFile, privateKeyFile, allRequestParams);
            return ResponseEntity.ok(output);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Erro ao executar playbook: " + e.getMessage());
        }
    }
}
