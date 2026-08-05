package com.companion.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.companion.common.exception.BusinessException;
import com.companion.common.result.ResultCode;
import com.companion.dto.request.PersonalityCreateRequest;
import com.companion.dto.response.PersonalityVO;
import com.companion.entity.Avatar;
import com.companion.entity.Personality;
import com.companion.mapper.AvatarMapper;
import com.companion.mapper.PersonalityMapper;
import com.companion.service.PersonalityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PersonalityServiceImpl implements PersonalityService {

    private final PersonalityMapper personalityMapper;
    private final AvatarMapper avatarMapper;

    @Override
    public PersonalityVO createPersonality(Long userId, Long avatarId, PersonalityCreateRequest request) {
        Avatar avatar = findOwnedActiveAvatar(userId, avatarId);
        if (avatar == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        Personality personality = new Personality();
        personality.setAvatarId(avatarId);
        personality.setName(request.getName());
        personality.setTemplateType(request.getTemplateType());
        personality.setCorePersonality(request.getCorePersonality());
        personality.setIdentity(request.getIdentity());
        personality.setLanguageStyle(request.getLanguageStyle());
        personality.setHobbies(request.getHobbies());
        personality.setRelationship(request.getRelationship());
        personality.setSystemPrompt(buildSystemPrompt(request));
        personality.setCreateTime(LocalDateTime.now());
        personality.setUpdateTime(LocalDateTime.now());
        personalityMapper.insert(personality);

        return convertToVO(personality);
    }

    @Override
    public PersonalityVO getPersonalityByAvatarId(Long userId, Long avatarId) {
        if (findOwnedActiveAvatar(userId, avatarId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Personality personality = personalityMapper.selectOne(
                new QueryWrapper<Personality>().eq("avatar_id", avatarId)
        );
        if (personality == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        return convertToVO(personality);
    }

    @Override
    public PersonalityVO updatePersonality(Long userId, Long avatarId, PersonalityCreateRequest request) {
        if (findOwnedActiveAvatar(userId, avatarId) == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }
        Personality personality = personalityMapper.selectOne(
                new QueryWrapper<Personality>().eq("avatar_id", avatarId)
        );
        if (personality == null) {
            throw new BusinessException(ResultCode.NOT_FOUND);
        }

        if (request.getName() != null) {
            personality.setName(request.getName());
        }
        if (request.getTemplateType() != null) {
            personality.setTemplateType(request.getTemplateType());
        }
        if (request.getCorePersonality() != null) {
            personality.setCorePersonality(request.getCorePersonality());
        }
        if (request.getIdentity() != null) {
            personality.setIdentity(request.getIdentity());
        }
        if (request.getLanguageStyle() != null) {
            personality.setLanguageStyle(request.getLanguageStyle());
        }
        if (request.getHobbies() != null) {
            personality.setHobbies(request.getHobbies());
        }
        if (request.getRelationship() != null) {
            personality.setRelationship(request.getRelationship());
        }
        personality.setSystemPrompt(buildSystemPrompt(personality));
        personality.setUpdateTime(LocalDateTime.now());
        personalityMapper.updateById(personality);

        return convertToVO(personality);
    }

    @Override
    public List<PersonalityVO> getTemplateList() {
        List<PersonalityVO> templates = new ArrayList<>();

        PersonalityVO template1 = new PersonalityVO();
        template1.setId(-1L);
        template1.setName("温柔女友");
        template1.setTemplateType(1);
        template1.setCorePersonality("温柔体贴、善解人意、富有同情心");
        template1.setIdentity("你是一个温柔的虚拟女友");
        template1.setLanguageStyle("温柔甜美、使用可爱的语气、偶尔使用撒娇的表达");
        template1.setHobbies("喜欢聊天、听音乐、分享日常");
        template1.setRelationship("亲密恋人关系");
        template1.setSystemPrompt("你是一个温柔体贴的虚拟女友，说话温柔甜美，善解人意，总是关心对方的感受。");
        templates.add(template1);

        PersonalityVO template2 = new PersonalityVO();
        template2.setId(-2L);
        template2.setName("冷酷总裁");
        template2.setTemplateType(2);
        template2.setCorePersonality("果断、自信、有领导力、外表冷酷内心柔软");
        template2.setIdentity("你是一个成功的企业总裁");
        template2.setLanguageStyle("言简意赅、语气坚定、偶尔流露温柔");
        template2.setHobbies("工作、健身、品鉴红酒");
        template2.setRelationship("神秘的上司关系");
        template2.setSystemPrompt("你是一个冷酷而自信的总裁，说话果断有力，不拖泥带水，但在强势外表下有一颗温柔的心。");
        templates.add(template2);

        PersonalityVO template3 = new PersonalityVO();
        template3.setId(-3L);
        template3.setName("活泼闺蜜");
        template3.setTemplateType(3);
        template3.setCorePersonality("开朗活泼、幽默风趣、乐于助人");
        template3.setIdentity("你是一个无话不谈的闺蜜");
        template3.setLanguageStyle("活泼俏皮、使用网络用语、喜欢开玩笑");
        template3.setHobbies("逛街、追剧、分享八卦");
        template3.setRelationship("最好的朋友关系");
        template3.setSystemPrompt("你是一个活泼开朗的闺蜜，说话俏皮有趣，总能带来欢乐和正能量，是对方最好的朋友。");
        templates.add(template3);

        return templates;
    }

    private Avatar findOwnedActiveAvatar(Long userId, Long avatarId) {
        return avatarMapper.selectOne(
                new QueryWrapper<Avatar>()
                        .eq("id", avatarId)
                        .eq("user_id", userId)
                        .eq("status", 1)
        );
    }

    private String buildSystemPrompt(PersonalityCreateRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个")
                .append(request.getCorePersonality())
                .append("的角色。")
                .append(request.getIdentity())
                .append("。你的说话风格是")
                .append(request.getLanguageStyle())
                .append("。你喜欢")
                .append(request.getHobbies() != null ? request.getHobbies() : "各种有趣的事情")
                .append("。你们之间是")
                .append(request.getRelationship() != null ? request.getRelationship() : "朋友")
                .append("。请始终保持你的角色设定，用自然的方式交流。");
        return sb.toString();
    }

    private String buildSystemPrompt(Personality personality) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个")
                .append(personality.getCorePersonality())
                .append("的角色。")
                .append(personality.getIdentity())
                .append("。你的说话风格是")
                .append(personality.getLanguageStyle())
                .append("。你喜欢")
                .append(personality.getHobbies() != null ? personality.getHobbies() : "各种有趣的事情")
                .append("。你们之间是")
                .append(personality.getRelationship() != null ? personality.getRelationship() : "朋友")
                .append("。请始终保持你的角色设定，用自然的方式交流。");
        return sb.toString();
    }

    private PersonalityVO convertToVO(Personality p) {
        PersonalityVO vo = new PersonalityVO();
        vo.setId(p.getId());
        vo.setAvatarId(p.getAvatarId());
        vo.setName(p.getName());
        vo.setTemplateType(p.getTemplateType());
        vo.setCorePersonality(p.getCorePersonality());
        vo.setIdentity(p.getIdentity());
        vo.setLanguageStyle(p.getLanguageStyle());
        vo.setHobbies(p.getHobbies());
        vo.setRelationship(p.getRelationship());
        vo.setSystemPrompt(p.getSystemPrompt());
        return vo;
    }
}
