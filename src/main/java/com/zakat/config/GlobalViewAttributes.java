package com.zakat.config;

import org.springframework.boot.info.BuildProperties;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

@ControllerAdvice
@Component
public class GlobalViewAttributes {

    private static final ZoneId DEFAULT_ZONE = ZoneId.of("Asia/Jakarta");
    private static final DateTimeFormatter BUILD_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(DEFAULT_ZONE);

    private final BuildProperties buildProperties;

    public GlobalViewAttributes(@Nullable BuildProperties buildProperties) {
        this.buildProperties = buildProperties;
    }

    @ModelAttribute("appVersion")
    public String appVersion() {
        String version = buildProperties == null ? "dev" : buildProperties.getVersion();
        String buildTime = buildProperties == null || buildProperties.getTime() == null
                ? "unknown-time"
                : BUILD_TIME_FORMATTER.format(buildProperties.getTime());
        String gitHash = resolveGitShortHash();
        return gitHash == null || gitHash.isBlank()
                ? String.format("%s (%s)", version, buildTime)
                : String.format("%s (%s, %s)", version, gitHash, buildTime);
    }

    private String resolveGitShortHash() {
        String fromGitProperties = readGitHashFromProperties();
        if (fromGitProperties != null && !fromGitProperties.isBlank()) {
            return fromGitProperties;
        }
        return readGitHashFromCommand();
    }

    private String readGitHashFromProperties() {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("git.properties")) {
            if (in == null) {
                return null;
            }
            Properties props = new Properties();
            props.load(in);
            String value = props.getProperty("git.commit.id.abbrev");
            return value == null || value.isBlank() ? null : value.trim();
        } catch (IOException e) {
            return null;
        }
    }

    private String readGitHashFromCommand() {
        try {
            Process process = new ProcessBuilder("git", "rev-parse", "--short", "HEAD")
                    .redirectErrorStream(true)
                    .start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return null;
            }
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8).trim();
            return output.isBlank() ? null : output;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return null;
        }
    }
}
