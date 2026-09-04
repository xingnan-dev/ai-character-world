package com.companion.ai.json;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts one unambiguous, complete JSON object from an LLM text response.
 * The caller remains responsible for DTO schema and business validation.
 */
public final class LlmJsonObjectExtractor {

    private static final String FENCE = "```";
    private static final ObjectMapper STRICT_MAPPER = new ObjectMapper()
            .enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);

    private LlmJsonObjectExtractor() {
    }

    public static String extract(String content) {
        if (content == null || content.isBlank()) {
            throw invalidOutput();
        }

        List<FencedBlock> fencedBlocks = fencedBlocks(content);
        if (!fencedBlocks.isEmpty()) {
            if (fencedBlocks.size() != 1) {
                throw invalidOutput();
            }
            FencedBlock block = fencedBlocks.get(0);
            String json = singleStrictObject(content.substring(block.contentStart(), block.contentEnd()), true);
            if (containsStrictObject(content.substring(0, block.openingStart()))
                    || containsStrictObject(content.substring(block.closingEnd()))) {
                throw invalidOutput();
            }
            return json;
        }
        return singleStrictObject(content, false);
    }

    private static List<FencedBlock> fencedBlocks(String content) {
        List<FencedBlock> blocks = new ArrayList<>();
        FencedBlockStart opening = null;
        int lineStart = 0;
        while (lineStart <= content.length()) {
            int newline = content.indexOf('\n', lineStart);
            int lineEnd = newline < 0 ? content.length() : newline;
            int visibleEnd = lineEnd > lineStart && content.charAt(lineEnd - 1) == '\r' ? lineEnd - 1 : lineEnd;
            String line = content.substring(lineStart, visibleEnd).trim();

            if (opening == null && (FENCE.equals(line) || "```json".equalsIgnoreCase(line))) {
                opening = new FencedBlockStart(lineStart, newline < 0 ? lineEnd : newline + 1);
            } else if (opening != null && FENCE.equals(line)) {
                blocks.add(new FencedBlock(opening.openingStart(), opening.contentStart(), lineStart,
                        newline < 0 ? lineEnd : newline + 1));
                opening = null;
            }

            if (newline < 0) {
                break;
            }
            lineStart = newline + 1;
        }
        if (opening != null) {
            throw invalidOutput();
        }
        return blocks;
    }

    private static String singleStrictObject(String text, boolean requireWhitespaceAroundObject) {
        String trimmed = text.trim();
        if (trimmed.startsWith("[")) {
            throw invalidOutput();
        }

        List<Candidate> candidates = strictObjectCandidates(text);
        if (candidates.size() != 1) {
            throw invalidOutput();
        }
        Candidate candidate = candidates.get(0);
        if (candidate.insideArray()
                || (requireWhitespaceAroundObject
                && (!text.substring(0, candidate.start()).isBlank()
                || !text.substring(candidate.end()).isBlank()))) {
            throw invalidOutput();
        }
        return candidate.json();
    }

    private static boolean containsStrictObject(String text) {
        return !strictObjectCandidates(text).isEmpty();
    }

    private static List<Candidate> strictObjectCandidates(String text) {
        List<Candidate> candidates = new ArrayList<>(2);
        int arrayDepth = 0;
        boolean outsideString = false;
        boolean outsideEscaped = false;

        for (int index = 0; index < text.length(); index++) {
            char current = text.charAt(index);
            if (outsideString) {
                if (outsideEscaped) {
                    outsideEscaped = false;
                } else if (current == '\\') {
                    outsideEscaped = true;
                } else if (current == '"') {
                    outsideString = false;
                }
                continue;
            }
            if (current == '"') {
                outsideString = true;
                continue;
            }
            if (current == '[') {
                arrayDepth++;
                continue;
            }
            if (current == ']' && arrayDepth > 0) {
                arrayDepth--;
                continue;
            }
            if (current != '{') {
                continue;
            }

            int end = matchingObjectEnd(text, index);
            if (end < 0) {
                break;
            }
            String candidate = text.substring(index, end);
            if (isStrictObject(candidate)) {
                candidates.add(new Candidate(index, end, candidate, arrayDepth > 0));
                index = end - 1;
                if (candidates.size() > 1) {
                    return candidates;
                }
            }
        }
        return candidates;
    }

    private static int matchingObjectEnd(String text, int start) {
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int index = start; index < text.length(); index++) {
            char current = text.charAt(index);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (current == '\\') {
                    escaped = true;
                } else if (current == '"') {
                    inString = false;
                }
            } else if (current == '"') {
                inString = true;
            } else if (current == '{') {
                depth++;
            } else if (current == '}' && --depth == 0) {
                return index + 1;
            }
        }
        return -1;
    }

    private static boolean isStrictObject(String candidate) {
        try {
            JsonNode root = STRICT_MAPPER.readTree(candidate);
            return root != null && root.isObject();
        } catch (JsonProcessingException exception) {
            return false;
        }
    }

    private static IllegalArgumentException invalidOutput() {
        return new IllegalArgumentException("LLM response does not contain one complete JSON object");
    }

    private record FencedBlockStart(int openingStart, int contentStart) {
    }

    private record FencedBlock(int openingStart, int contentStart, int contentEnd, int closingEnd) {
    }

    private record Candidate(int start, int end, String json, boolean insideArray) {
    }
}
