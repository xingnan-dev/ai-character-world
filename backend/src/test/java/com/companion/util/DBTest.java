package com.companion.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * 数据库连接测试工具
 */
public class DBTest {
    public static void main(String[] args) {
        String[] urls = {
            "jdbc:mysql://127.0.0.1:3307/ai_virtual_companion?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
            "jdbc:mysql://localhost:3307/ai_virtual_companion?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Shanghai",
            "jdbc:mysql://127.0.0.1:3307/ai_virtual_companion",
            "jdbc:mysql://127.0.0.1:3306/ai_virtual_companion"
        };
        String username = "root";
        String password = "123456";

        for (String url : urls) {
            System.out.println("尝试连接: " + url);
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
                Connection conn = DriverManager.getConnection(url, username, password);
                System.out.println("✓ 连接成功!");
                
                Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT 1 AS test");
                if (rs.next()) {
                    System.out.println("✓ 查询测试成功: " + rs.getInt("test"));
                }
                rs.close();
                stmt.close();
                conn.close();
                break;
            } catch (Exception e) {
                System.out.println("✗ 连接失败: " + e.getMessage());
            }
            System.out.println();
        }
    }
}
