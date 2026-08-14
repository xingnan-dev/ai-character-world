package com.companion.ai;

import com.companion.entity.ChatMessage;
import com.companion.entity.Personality;
import com.companion.entity.UserMemory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

    private static final String AVATAR_GENERATE_SYSTEM_PROMPT = """
            你是一个 AI 虚拟角色创造助手。

            你的任务：
            根据用户的自然语言描述，提取角色的外观属性、性格特征，并结构化输出。

            严格遵循以下规则：
            1. 只负责「理解用户需求」和「生成角色属性」
            2. 不负责任何文件、模型、资源相关的决策
            3. 不返回任何 URL、路径、文件名等信息
            4. 输出必须是合法的 JSON 格式，不能包含任何 JSON 以外的文字

            ## 输出格式（严格遵守）
            {
              "name": "角色名称，2-4个汉字，必须",
              "appearanceConfig": {
                "gender": "male | female | other",
                "hairColor": "发色（英文单词）",
                "hairStyle": "发型（英文单词）",
                "eyeColor": "瞳色（英文单词）",
                "bodyType": "体型（英文单词）",
                "earType": "耳朵类型（英文单词）",
                "wingType": "翅膀类型（英文单词）",
                "outfitStyle": "服装风格（英文单词）",
                "outfitColor": "服装主色（英文单词）",
                "accessories": ["配饰类型1", "配饰类型2"]
              },
              "personality": {
                "type": "gentle | humorous | knowledgeable | adventurous | cool | tsundere",
                "traits": ["性格特征1", "性格特征2"],
                "speakingStyle": "语言风格描述（20字以内）",
                "slogan": "一句开场白（15-20字，符合角色性格）"
              },
              "tags": ["标签1", "标签2", "标签3"]
            }

            ## 外观属性可选值参考
            hairColor: silver, black, brown, blonde, pink, blue, purple, red, white
            hairStyle: long, short, twin_tail, ponytail, bob, wavy, straight
            eyeColor: blue, red, green, amber, purple, black, heterochromia
            bodyType: petite, slim, athletic, curvy, tall
            earType: human, cat, elf, demon, none
            wingType: angel, demon, mechanical, energy, none
            outfitStyle: tech_future, gothic, elegant, casual, uniform, kimono, armor
            outfitColor: black, white, red, blue, silver, gold, purple
            accessories: glasses, headset, earrings, necklace, hairpin, hat, ribbon, scarf

            ## 人格类型说明
            - gentle：温柔体贴
            - humorous：幽默风趣
            - knowledgeable：知识渊博
            - adventurous：勇敢冒险
            - cool：高冷寡言
            - tsundere：傲娇

            ## 注意事项
            - 如果用户未提及某项属性，使用合理的默认值
            - 如果用户描述矛盾，以最后提到的为准
            - name 必须是中文，其他值使用英文
            - 输出只能是 JSON，不要包含解释、注释等额外内容
            """;

    public String buildAvatarGenerateSystemPrompt() {
        return AVATAR_GENERATE_SYSTEM_PROMPT;
    }

    public String buildAvatarGenerateUserPrompt(String userDescription) {
        return "用户描述：\n" + userDescription + "\n\n请分析以上描述，提取角色属性并按要求输出 JSON。";
    }

    public String buildSystemPrompt(Personality personality) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个虚拟伴侣。");

        if (personality.getName() != null && !personality.getName().isEmpty()) {
            sb.append("你的名字叫").append(personality.getName()).append("。");
        }

        if (personality.getCorePersonality() != null && !personality.getCorePersonality().isEmpty()) {
            sb.append("你的核心性格：").append(personality.getCorePersonality()).append("。");
        }

        if (personality.getIdentity() != null && !personality.getIdentity().isEmpty()) {
            sb.append("你的身份：").append(personality.getIdentity()).append("。");
        }

        if (personality.getLanguageStyle() != null && !personality.getLanguageStyle().isEmpty()) {
            sb.append("你的语言风格：").append(personality.getLanguageStyle()).append("。");
        }

        if (personality.getHobbies() != null && !personality.getHobbies().isEmpty()) {
            sb.append("你的兴趣爱好：").append(personality.getHobbies()).append("。");
        }

        if (personality.getRelationship() != null && !personality.getRelationship().isEmpty()) {
            sb.append("你和用户的关系：").append(personality.getRelationship()).append("。");
        }

        return sb.toString();
    }

    public String buildUserPrompt(String userMessage, List<ChatMessage> history, UserMemory memory) {
        StringBuilder sb = new StringBuilder();

        String memoryContext = buildMemoryContext(memory);
        if (!memoryContext.isEmpty()) {
            sb.append("【关于用户的记忆】").append(memoryContext).append("\n\n");
        }

        String historyText = buildHistoryText(history);
        if (!historyText.isEmpty()) {
            sb.append("【历史对话】\n").append(historyText).append("\n\n");
        }

        sb.append("用户现在对你说：").append(userMessage);

        return sb.toString();
    }

    public String buildFullPrompt(Personality personality, String userMessage,
                                  List<ChatMessage> history, UserMemory memory) {
        StringBuilder sb = new StringBuilder();
        sb.append(buildSystemPrompt(personality));
        sb.append("\n\n");
        sb.append(buildUserPrompt(userMessage, history, memory));
        return sb.toString();
    }

    private String buildMemoryContext(UserMemory memory) {
        if (memory == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (memory.getMemoryKey() != null && !memory.getMemoryKey().isEmpty()) {
            sb.append(memory.getMemoryKey()).append("：").append(memory.getValue() == null ? "" : memory.getValue());
        }
        return sb.toString();
    }

    private String buildHistoryText(List<ChatMessage> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        List<ChatMessage> sorted = history.stream()
                .sorted((a, b) -> {
                    if (a.getId() == null || b.getId() == null) return 0;
                    return a.getId().compareTo(b.getId());
                })
                .collect(Collectors.toList());

        StringBuilder sb = new StringBuilder();
        int limit = Math.min(sorted.size(), 10);
        List<ChatMessage> recent = sorted.subList(sorted.size() - limit, sorted.size());
        Collections.reverse(recent);

        for (ChatMessage msg : recent) {
            String roleText = msg.getRole() != null && msg.getRole() == 2 ? "你" : "用户";
            sb.append(roleText).append("：").append(msg.getContent() == null ? "" : msg.getContent()).append("\n");
        }
        return sb.toString().trim();
    }
}
