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

    public String executePlaybook(MultipartFile playbook, MultipartFile privateKey, Map<String, String> parameters)
            throws IOException, InterruptedException {

        Path playbookPath = Files.createTempFile("playbook-", ".yml");
        Path privateKeyPath = Files.createTempFile("key-", ".pem");

        try {
            // Salva os arquivos
            Files.write(playbookPath, playbook.getBytes());
            Files.write(privateKeyPath, privateKey.getBytes());
            Files.setPosixFilePermissions(privateKeyPath, Set.of(PosixFilePermission.OWNER_READ));

            // Constrói extra vars
            String extraVars = "";
            if (!parameters.isEmpty()) {
                extraVars = parameters.entrySet().stream()
                        .map(entry -> entry.getKey() + "=" + entry.getValue())
                        .collect(Collectors.joining(" "));
            }

            // Detecta SO
            String os = System.getProperty("os.name").toLowerCase();
            boolean isWindows = os.contains("win");

            // Ajusta paths e comando conforme o SO
            String command;
            if (isWindows) {
                String playbookWsl = convertToWslPath(playbookPath.toAbsolutePath().toString());
                String keyWsl = convertToWslPath(privateKeyPath.toAbsolutePath().toString());

                 command = "wsl ansible-playbook " + playbookWsl + " --private-key " + keyWsl;
                    if (!extraVars.isEmpty()) {
                        command += " --extra-vars \"" + extraVars + "\"";
                    }
                } else {
                    command = "ansible-playbook " + playbookPath + " --private-key " + privateKeyPath;
                    if (!extraVars.isEmpty()) {
                        command += " --extra-vars \"" + extraVars + "\"";
                    }
                }

                // Executa comando
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();

                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();

                return "Código de saída: " + exitCode + "\n\n" + output;

            } finally {
                // Remove arquivos temporários
                Files.deleteIfExists(playbookPath);
                Files.deleteIfExists(privateKeyPath);
            }
        }

        private String convertToWslPath(String windowsPath) {
            return windowsPath
                    .replace("C:", "/mnt/c")
                    .replace("\\", "/");
        }

}
