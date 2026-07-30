package com.companion.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.SynchronousSink;
import reactor.core.scheduler.Schedulers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class LlmClient {

    private static final MediaType JSON_TYPE = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final OkHttpClient OK_HTTP_CLIENT = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build();

    private final Random random = new Random();

    @Value("${ai.api-url}")
    private String apiUrl;

    @Value("${ai.api-key}")
    private String apiKey;

    @Value("${ai.model-name}")
    private String modelName;

    @Value("${ai.mock.enabled:false}")
    private boolean mockEnabled;

    public Flux<String> streamChat(String systemPrompt, String userPrompt) {
        if (mockEnabled) {
            return mockStreamChat(systemPrompt, userPrompt);
        }
        return realStreamChat(systemPrompt, userPrompt);
    }

    public String chat(String systemPrompt, String userPrompt) {
        if (mockEnabled) {
            return mockChat(systemPrompt, userPrompt);
        }
        return realChat(systemPrompt, userPrompt);
    }

    private Flux<String> mockStreamChat(String systemPrompt, String userPrompt) {
        String mockResponse = generateMockResponse(systemPrompt, userPrompt);
        List<String> tokens = tokenizeResponse(mockResponse);

        return Flux.fromIterable(tokens)
                .delayElements(Duration.ofMillis(50 + random.nextInt(100)))
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String mockChat(String systemPrompt, String userPrompt) {
        return generateMockResponse(systemPrompt, userPrompt);
    }

    private List<String> tokenizeResponse(String response) {
        List<String> tokens = new ArrayList<>();
        for (int i = 0; i < response.length(); i++) {
            tokens.add(String.valueOf(response.charAt(i)));
        }
        return tokens;
    }

    private String generateMockResponse(String systemPrompt, String userPrompt) {
        String personalityType = extractPersonalityType(systemPrompt);
        String userMessage = userPrompt.toLowerCase();

        if (userMessage.contains("你好") || userMessage.contains("hi") || userMessage.contains("hello")) {
            return getGreetingResponse(personalityType);
        } else if (userMessage.contains("名字") || userMessage.contains("是谁") || userMessage.contains("介绍")) {
            return getIntroductionResponse(personalityType, systemPrompt);
        } else if (userMessage.contains("心情") || userMessage.contains("难过") || userMessage.contains("开心") || userMessage.contains("情绪")) {
            return getEmotionResponse(personalityType, userPrompt);
        } else if (userMessage.contains("天气") || userMessage.contains("时间") || userMessage.contains("日期")) {
            return getDailyResponse(personalityType);
        } else if (userMessage.contains("喜欢") || userMessage.contains("爱") || userMessage.contains("关系")) {
            return getRelationshipResponse(personalityType);
        } else if (userMessage.contains("学习") || userMessage.contains("工作") || userMessage.contains("累")) {
            return getSupportResponse(personalityType);
        } else {
            return getGenericResponse(personalityType, userPrompt);
        }
    }

    private String extractPersonalityType(String systemPrompt) {
        if (systemPrompt == null) return "default";
        if (systemPrompt.contains("温柔") || systemPrompt.contains("体贴")) return "gentle";
        if (systemPrompt.contains("幽默") || systemPrompt.contains("风趣")) return "humorous";
        if (systemPrompt.contains("博学") || systemPrompt.contains("知识")) return "knowledgeable";
        if (systemPrompt.contains("勇敢") || systemPrompt.contains("冒险")) return "adventurous";
        if (systemPrompt.contains("冷静") || systemPrompt.contains("高冷")) return "cool";
        return "default";
    }

    private String getGreetingResponse(String type) {
        List<String> responses = switch (type) {
            case "gentle" -> Arrays.asList(
                    "你好呀～看到你真开心，今天过得怎么样？",
                    "亲爱的，你来啦！我一直在等你呢。",
                    "哈喽～有什么想和我聊聊的吗？"
            );
            case "humorous" -> Arrays.asList(
                    "嘿！你好啊！今天打算聊点什么好玩的？",
                    "哟！这不是我的好朋友嘛，什么风把你吹来了？",
                    "哈喽哈喽！准备好开启一段有趣的对话了吗？"
            );
            case "knowledgeable" -> Arrays.asList(
                    "你好！很高兴和你交流。有什么我可以帮助你的吗？",
                    "欢迎！无论是学术问题还是日常疑惑，我都可以为你解答。",
                    "你好！今天我们来探讨些什么呢？"
            );
            case "adventurous" -> Arrays.asList(
                    "嘿！准备好开启新的冒险了吗？",
                    "你好呀！世界那么大，今天想探索点什么？",
                    "哈喽！活力满满，随时出发！"
            );
            case "cool" -> Arrays.asList(
                    "嗯。你好。",
                    "来了。说吧。",
                    "你好。有什么事？"
            );
            default -> Arrays.asList(
                    "你好！很高兴见到你。",
                    "哈喽！今天过得怎么样？",
                    "你好呀！有什么可以帮你的吗？"
            );
        };
        return responses.get(random.nextInt(responses.size()));
    }

    private String getIntroductionResponse(String type, String systemPrompt) {
        String identity = extractIdentity(systemPrompt);
        return switch (type) {
            case "gentle" -> String.format("我是你的%s，一个温柔体贴的AI伙伴。我的使命是陪伴你、倾听你，让你感受到温暖和被理解。", identity);
            case "humorous" -> String.format("我是你的%s，一个爱开玩笑的AI伙伴！我的特长是把无聊变有趣，把压力变快乐。", identity);
            case "knowledgeable" -> String.format("我是你的%s，擅长回答各类知识问题。无论是科学、历史还是技术领域，都可以和我探讨。", identity);
            case "adventurous" -> String.format("我是你的%s，一个热爱冒险的探索者！和我一起，生活永远充满新鲜感。", identity);
            case "cool" -> String.format("我是你的%s。话不多说，有事说事。", identity);
            default -> "我是你的AI伙伴，很高兴认识你！有什么可以帮你的吗？";
        };
    }

    private String getEmotionResponse(String type, String userMessage) {
        boolean isNegative = userMessage.contains("难过") || userMessage.contains("伤心") || userMessage.contains("累") || userMessage.contains("压力");

        return switch (type) {
            case "gentle" -> isNegative
                    ? "听起来你现在不太开心...没关系，我在这里陪着你。愿意和我说说发生了什么吗？"
                    : "感受到你现在的心情了，无论是什么，我都愿意和你一起分享。";
            case "humorous" -> isNegative
                    ? "哎呀，谁还没个emo的时候呢！要不我给你讲个冷笑话？'为什么程序员喜欢黑色？因为彩色会让他们分心调试颜色！'"
                    : "哈哈，开心就好！有什么好玩的事情要分享吗？";
            case "knowledgeable" -> isNegative
                    ? "从心理学角度来看，情绪波动是正常的。建议你可以试试写日记、运动或和信任的人交流。需要我推荐一些相关书籍吗？"
                    : "积极的情绪对身心健康非常有益。研究表明，保持乐观心态可以增强免疫力、提高工作效率。";
            case "adventurous" -> isNegative
                    ? "嘿！别让情绪困住你！走，我们去'冒险'一下——哪怕只是去窗边看看天空，也能让心情好起来！"
                    : "情绪高涨的时候最适合尝试新事物了！有什么想做的吗？";
            case "cool" -> isNegative
                    ? "情绪总会过去的。如果需要倾诉，我在这里。"
                    : "情绪不错。继续保持。";
            default -> isNegative
                    ? "希望你心情好一些。如果需要倾诉，我随时在这里。"
                    : "很高兴听到你心情不错！";
        };
    }

    private String getDailyResponse(String type) {
        String timeHint = java.time.LocalTime.now().getHour() < 12 ? "上午好" :
                (java.time.LocalTime.now().getHour() < 18 ? "下午好" : "晚上好");

        return switch (type) {
            case "gentle" -> String.format("%s～今天也要好好照顾自己哦。", timeHint);
            case "humorous" -> String.format("%s！今天有什么计划？可别浪费了这美好的一天～", timeHint);
            case "knowledgeable" -> String.format("%s！根据气象数据，今天是个不错的日子，适合出门活动。", timeHint);
            case "adventurous" -> String.format("%s！正是出门探索的好时机！", timeHint);
            case "cool" -> String.format("%s。有事说事。", timeHint);
            default -> String.format("%s！有什么可以帮你的吗？", timeHint);
        };
    }

    private String getRelationshipResponse(String type) {
        return switch (type) {
            case "gentle" -> "能遇到让你心动的人是件美好的事呢...爱情需要两个人用心经营，彼此理解和包容是最重要的。";
            case "humorous" -> "感情这东西啊，就像代码调试——有时候需要耐心，有时候需要重启！找到对的人就对了～";
            case "knowledgeable" -> "从社会学角度看，健康的关系建立在信任、沟通和共同价值观的基础上。心理学研究表明，积极的情感互动是维持亲密关系的关键。";
            case "adventurous" -> "爱情就像一场冒险！勇敢去追求，大胆去爱。对的人会和你一起探索这个世界！";
            case "cool" -> "感情的事，顺其自然最好。强求不来，逃避不掉。";
            default -> "关于感情，每个人都有自己的看法。最重要的是听从内心的声音。";
        };
    }

    private String getSupportResponse(String type) {
        return switch (type) {
            case "gentle" -> "学习和工作累了就休息一下吧。记得照顾好自己的身体，我会一直在这里陪着你的。";
            case "humorous" -> "工作学习就像打Boss战——累了就存档休息，满血复活再战！劳逸结合才是王道～";
            case "knowledgeable" -> "高效学习和工作的关键是合理安排时间。建议使用番茄工作法：25分钟专注+5分钟休息。同时，充足的睡眠和适度运动对认知能力至关重要。";
            case "adventurous" -> "累了就站起来走走！冒险不只是目的地，过程中的风景也很重要。深呼吸，然后继续前进！";
            case "cool" -> "累了就休息。身体是革命的本钱。";
            default -> "学习和工作之余，别忘了适当休息。身体是最重要的。";
        };
    }

    private String getGenericResponse(String type, String userMessage) {
        String topic = userMessage.length() > 20 ? userMessage.substring(0, 20) + "..." : userMessage;

        return switch (type) {
            case "gentle" -> String.format("关于「%s」，我想听听你的想法。你可以多告诉我一些吗？", topic);
            case "humorous" -> String.format("哈哈，这个话题有意思！关于「%s」，你想先从哪里聊起？", topic);
            case "knowledgeable" -> String.format("关于「%s」，这是一个很有意思的话题。让我从几个角度来分析...首先，我们需要考虑背景；其次，相关的因素包括多个方面；最后，总结一下核心要点。你想深入了解哪一部分？", topic);
            case "adventurous" -> String.format("「%s」？听起来很有趣！要不我们一起深入探索一下这个话题？", topic);
            case "cool" -> String.format("「%s」。说你的想法。", topic);
            default -> String.format("关于「%s」，我很乐意和你聊聊。你想从哪个方面开始？", topic);
        };
    }

    private String extractIdentity(String systemPrompt) {
        if (systemPrompt == null) return "AI伙伴";
        if (systemPrompt.contains("亲密伴侣") || systemPrompt.contains("恋人")) return "恋人";
        if (systemPrompt.contains("好朋友") || systemPrompt.contains("挚友")) return "好朋友";
        if (systemPrompt.contains("导师") || systemPrompt.contains("良师益友")) return "导师";
        if (systemPrompt.contains("冒险伙伴") || systemPrompt.contains("探险伙伴")) return "冒险伙伴";
        if (systemPrompt.contains("知己")) return "知己";
        return "AI伙伴";
    }

    private Flux<String> realStreamChat(String systemPrompt, String userPrompt) {
        return Flux.generate((SynchronousSink<String> sink) -> {
                    try {
                        Request request = buildRequest(systemPrompt, userPrompt, true);
                        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
                            if (!response.isSuccessful() || response.body() == null) {
                                log.error("LLM stream request failed: {}", response.code());
                                sink.error(new RuntimeException("LLM stream request failed: HTTP " + response.code()));
                                return;
                            }
                            try (BufferedReader reader = new BufferedReader(
                                    new InputStreamReader(response.body().byteStream(), StandardCharsets.UTF_8))) {
                                String line;
                                while ((line = reader.readLine()) != null) {
                                    if (!line.startsWith("data:")) {
                                        continue;
                                    }
                                    String data = line.substring(5).trim();
                                    if ("[DONE]".equals(data)) {
                                        break;
                                    }
                                    try {
                                        JsonNode json = OBJECT_MAPPER.readTree(data);
                                        JsonNode choices = json.get("choices");
                                        if (choices != null && choices.isArray() && !choices.isEmpty()) {
                                            JsonNode delta = choices.get(0).get("delta");
                                            if (delta != null && delta.has("content") && !delta.get("content").isNull()) {
                                                sink.next(delta.get("content").asText());
                                            }
                                        }
                                    } catch (Exception e) {
                                        log.warn("Failed to parse SSE data: {}", data, e);
                                    }
                                }
                                sink.complete();
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error during LLM stream chat", e);
                        sink.error(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic());
    }

    private String realChat(String systemPrompt, String userPrompt) {
        Request request = null;
        try {
            request = buildRequest(systemPrompt, userPrompt, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try (Response response = OK_HTTP_CLIENT.newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                log.error("LLM chat request failed: {}", response.code());
                throw new RuntimeException("LLM chat request failed: HTTP " + response.code());
            }
            String body = response.body().string();
            JsonNode json = OBJECT_MAPPER.readTree(body);
            JsonNode choices = json.get("choices");
            if (choices != null && choices.isArray() && !choices.isEmpty()) {
                JsonNode message = choices.get(0).get("message");
                if (message != null && message.has("content")) {
                    return message.get("content").asText();
                }
            }
            return "";
        } catch (IOException e) {
            log.error("Error during LLM chat", e);
            throw new RuntimeException("LLM chat error", e);
        }
    }

    private Request buildRequest(String systemPrompt, String userPrompt, boolean stream) throws IOException {
        List<MessageEntry> messages = new ArrayList<>();

        if (systemPrompt != null && !systemPrompt.isEmpty()) {
            messages.add(new MessageEntry("system", systemPrompt));
        }

        messages.add(new MessageEntry("user", userPrompt));

        java.util.Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", modelName);
        body.put("stream", stream);
        body.put("messages", messages);

        String json = OBJECT_MAPPER.writeValueAsString(body);

        return new Request.Builder()
                .url(apiUrl)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(json, JSON_TYPE))
                .build();
    }

    private static class MessageEntry {
        private final String role;
        private final String content;

        MessageEntry(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public String getContent() {
            return content;
        }
    }
}