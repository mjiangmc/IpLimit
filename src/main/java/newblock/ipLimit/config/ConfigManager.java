package newblock.ipLimit.config;

import com.google.inject.Inject;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import org.slf4j.Logger;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class ConfigManager {
    private final Path dataDirectory;
    private final Logger logger;
    private final Path configPath;
    private Map<String, Object> config;

    @Inject
    public ConfigManager(@DataDirectory Path dataDirectory, Logger logger) {
        this.dataDirectory = dataDirectory;
        this.logger = logger;
        this.configPath = dataDirectory.resolve("config.yml");
        this.config = new HashMap<>();
    }

    /**
     * 加载配置文件
     */
    public void loadConfig() {
        // 确保数据目录存在
        if (!Files.exists(dataDirectory)) {
            try {
                Files.createDirectories(dataDirectory);
            } catch (IOException e) {
                logger.error("无法创建数据目录", e);
                return;
            }
        }

        // 如果配置文件不存在，从资源中复制默认配置
        if (!Files.exists(configPath)) {
            try (InputStream in = getClass().getClassLoader().getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configPath);
                } else {
                    logger.error("无法找到默认配置文件");
                    return;
                }
            } catch (IOException e) {
                logger.error("无法创建配置文件", e);
                return;
            }
        }

        // 加载配置文件
        try (FileInputStream fis = new FileInputStream(configPath.toFile())) {
            Yaml yaml = new Yaml();
            config = yaml.load(fis);
            if (config == null) {
                config = new HashMap<>();
            }
            logger.info("配置文件已加载");
        } catch (IOException e) {
            logger.error("无法加载配置文件", e);
        }
    }

    /**
     * 重新加载配置文件
     */
    public void reloadConfig() {
        loadConfig();
    }

    /**
     * 获取每个IP地址允许的最大在线玩家数量
     */
    public int getMaxPlayersPerIp() {
        return getInt("max-players-per-ip", 3);
    }

    /**
     * 获取绕过IP限制的权限
     */
    public String getBypassPermission() {
        return getString("bypass-permission", "iplimit.bypass");
    }

    /**
     * 获取拒绝连接时显示的消息
     */
    public String getKickMessage() {
        return getString("kick-message", "§c同一IP地址最多只能有 {max} 个玩家同时在线！");
    }

    /**
     * 获取调试模式状态
     */
    public boolean isDebugEnabled() {
        return getBoolean("debug", false);
    }

    /**
     * 从配置中获取字符串值
     */
    private String getString(String path, String defaultValue) {
        Object value = config.get(path);
        return value instanceof String ? (String) value : defaultValue;
    }

    /**
     * 从配置中获取整数值
     */
    private int getInt(String path, int defaultValue) {
        Object value = config.get(path);
        return value instanceof Number ? ((Number) value).intValue() : defaultValue;
    }

    /**
     * 从配置中获取布尔值
     */
    private boolean getBoolean(String path, boolean defaultValue) {
        Object value = config.get(path);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }
}
