package com.akash.sentinel.rag;

import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class MarkdownRunbookSplitter {

    private static final Pattern H2_PATTERN = Pattern.compile("(?m)^## ");

    public List<Document> split(Path filePath) throws IOException {
        String raw = Files.readString(filePath, StandardCharsets.UTF_8);
        Map<String, Object> frontmatter = new HashMap<>();
        String body = raw;

        if (raw.startsWith("---")) {
            int end = raw.indexOf("---", 3);
            if (end != -1) {
                String yamlBlock = raw.substring(3, end).trim();
                frontmatter = parseYaml(yamlBlock);
                body = raw.substring(end + 3).trim();
            }
        }

        List<Document> chunks = new ArrayList<>();
        List<String> sections = splitOnH2(body);

        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) {
                continue;
            }

            String sectionName = "preamble";
            String content = section;

            if (section.startsWith("## ")) {
                int newline = section.indexOf('\n');
                if (newline == -1) {
                    newline = section.length();
                }
                sectionName = section.substring(3, newline).trim();
                content = section.substring(newline).trim();
            }

            String hash = sha256(content);
            Map<String, Object> meta = new HashMap<>(frontmatter);
            meta.put("section_name", sectionName);
            meta.put("content_hash", hash);
            meta.put("runbook_id", filePath.getFileName().toString().replace(".md", ""));

            chunks.add(new Document(content, meta));
        }

        return chunks;
    }

    private List<String> splitOnH2(String body) {
        List<String> parts = new ArrayList<>();
        Matcher m = H2_PATTERN.matcher(body);
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                parts.add(body.substring(last, m.start()));
            }
            last = m.start();
        }
        if (last < body.length()) {
            parts.add(body.substring(last));
        }
        return parts;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseYaml(String yaml) {
        try {
            Yaml parser = new Yaml();
            Object loaded = parser.load(yaml);
            if (loaded instanceof Map) {
                return (Map<String, Object>) loaded;
            }
        } catch (Exception e) {
            // Ignore malformed frontmatter; continue with empty metadata
        }
        return new HashMap<>();
    }

    private String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            return "0";
        }
    }
}
