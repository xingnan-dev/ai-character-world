package com.companion.controller;

import com.companion.common.result.Result;
import com.companion.entity.User;
import com.companion.mapper.UserMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/test")
public class TestController {

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @GetMapping("/db")
    public Result<Map<String, Object>> testDB() {
        Map<String, Object> result = new HashMap<>();
        try {
            Integer count = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
            result.put("jdbcTest", count);
            
            Long userCount = userMapper.selectCount(null);
            result.put("mybatisPlusTest", userCount);
            result.put("success", true);
            return Result.success(result);
        } catch (Exception e) {
            log.error("数据库连接测试失败", e);
            StringBuilder errMsg = new StringBuilder();
            errMsg.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage());
            Throwable cause = e.getCause();
            if (cause != null) {
                errMsg.append(" | Cause: ").append(cause.getClass().getSimpleName()).append(": ").append(cause.getMessage());
            }
            result.put("success", false);
            result.put("error", errMsg.toString());
            return Result.success(result);
        }
    }

    @GetMapping("/user")
    public Result<User> testUser() {
        try {
            User user = userMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.QueryWrapper<User>()
                    .eq("username", "test")
            );
            if (user != null) {
                user.setPassword(null);
            }
            return Result.success(user);
        } catch (Exception e) {
            log.error("查询用户失败", e);
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/raw")
    public Result<Map<String, Object>> testRawSQL() {
        Map<String, Object> result = new HashMap<>();
        try {
            java.util.List<java.util.Map<String, Object>> users = jdbcTemplate.queryForList("SELECT id, username, nickname, status FROM t_user LIMIT 10");
            result.put("success", true);
            result.put("users", users);
            return Result.success(result);
        } catch (Exception e) {
            log.error("原始SQL查询失败", e);
            result.put("success", false);
            result.put("error", e.getMessage());
            return Result.error(500, "查询失败: " + e.getMessage());
        }
    }

    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("OK");
    }

    @GetMapping("/ping")
    public Result<String> ping() {
        return Result.success("pong");
    }
}
