package newblock.ipLimit;

import com.google.inject.Inject;
import com.velocitypowered.api.command.CommandManager;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import newblock.ipLimit.command.IpLimitCommand;
import newblock.ipLimit.config.ConfigManager;
import newblock.ipLimit.listener.ConnectionListener;
import newblock.ipLimit.tracker.IpTracker;
import org.slf4j.Logger;

import java.nio.file.Path;

@Plugin(id = "iplimit", name = "IpLimit", version = "1.0-SNAPSHOT", url = "https://newblock.top", authors = {"NewBlockTeam"})
public class IpLimit {

    private final Logger logger;
    private final ProxyServer server;
    private final Path dataDirectory;
    private ConfigManager configManager;
    private IpTracker ipTracker;
    private ConnectionListener connectionListener;

    @Inject
    public IpLimit(Logger logger, ProxyServer server, @DataDirectory Path dataDirectory) {
        this.logger = logger;
        this.server = server;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // 初始化配置管理器
        configManager = new ConfigManager(dataDirectory, logger);
        configManager.loadConfig();

        // 初始化IP跟踪器
        ipTracker = new IpTracker(server, logger);
        ipTracker.setDebug(configManager.isDebugEnabled());

        // 注册事件监听器
        connectionListener = new ConnectionListener(configManager, ipTracker, logger);
        server.getEventManager().register(this, connectionListener);

        // 注册命令
        CommandManager commandManager = server.getCommandManager();
        commandManager.register("iplimit", new IpLimitCommand(configManager, ipTracker));

        logger.info("IpLimit 插件已加载，每个IP地址最多允许 {} 个玩家同时在线", configManager.getMaxPlayersPerIp());
    }

    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        logger.info("IpLimit 插件已卸载");
    }
}
