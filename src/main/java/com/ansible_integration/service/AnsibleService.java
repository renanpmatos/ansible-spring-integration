package com.ansible_integration.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class AnsibleService {
    private final Path storagePath = Paths.get("/opt/playbooks");

    public void savePlaybook(String name, MultipartFile file) throws IOException{
        if (!Files.exists(storagePath)){
            Files.createDirectories(storagePath);
        }
        Path destination = storagePath.resolve(name + ".yml");
        Files.copy(file.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean isValidYaml(MultipartFile file){
        try(InputStream input = file.getInputStream()){
            new Yaml(new SafeConstructor(new LoaderOptions())).load(input);
            return true;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String executePlaybook(String playbookName, Map<String, String> parameters, MultipartFile privateKey) throws IOException, InterruptedException {
        Path playbookDir = Paths.get("/opt/playbooks");
        Path playbookFile = playbookDir.resolve(playbookName + ".yml");

        // Criar chave temporária
        Path tempKeyFile = Files.createTempFile("temp-key", ".pem");
        Files.write(tempKeyFile, privateKey.getBytes());
        Files.setPosixFilePermissions(tempKeyFile, Set.of(PosixFilePermission.OWNER_READ));

        String command = "ansible-playbook " + playbookFile.toAbsolutePath() +
                " --private-key " + tempKeyFile.toAbsolutePath();

        if (!parameters.isEmpty()) {
            String extraVars = parameters.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.joining(" "));
            command += " --extra-vars \"" + extraVars + "\"";
        }

        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(playbookDir.toFile());

        Process process = pb.start();
        String output = new String(process.getInputStream().readAllBytes());
        process.waitFor();

        // Limpar chave temporária
        Files.deleteIfExists(tempKeyFile);

        return output;
    }


}
