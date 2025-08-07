package newblock.ipLimit.listener;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.ResultedEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.LoginEvent;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PreLoginEvent;
import com.velocitypowered.api.proxy.Player;
import newblock.ipLimit.config.ConfigManager;
import newblock.ipLimit.tracker.IpTracker;
import org.slf4j.Logger;

/**
 * 监听玩家连接事件
 */
public class ConnectionListener {
    private final ConfigManager configManager;
    private final IpTracker ipTracker;
    private final Logger logger;

    public ConnectionListener(ConfigManager configManager, IpTracker ipTracker, Logger logger) {
        this.configManager = configManager;
        this.ipTracker = ipTracker;
        this.logger = logger;
    }

    /**
     * 监听玩家预登录事件
     * 在这个阶段检查IP地址是否超过限制
     */
    @Subscribe(order = PostOrder.FIRST)
    public void onPreLogin(PreLoginEvent event) {
        // 获取玩家IP地址
        String ip = event.getConnection().getRemoteAddress().getAddress().getHostAddress();

        // 获取配置的最大玩家数量
        int maxPlayersPerIp = configManager.getMaxPlayersPerIp();

        // 检查IP地址是否超过限制
        if (ipTracker.isIpOverLimit(ip, maxPlayersPerIp)) {
            // 获取踢出消息
            String kickMessage = configManager.getKickMessage()
                    .replace("{max}", String.valueOf(maxPlayersPerIp));

            // 拒绝连接
            event.setResult(PreLoginEvent.PreLoginComponentResult.denied(
                    net.kyori.adventure.text.Component.text(kickMessage)));

            if (configManager.isDebugEnabled()) {
                logger.info("拒绝来自IP {} 的连接请求，该IP已达到最大在线玩家数量 {}", ip, maxPlayersPerIp);
            }
        }
    }

    /**
     * 监听玩家登录事件
     * 在这个阶段检查玩家是否有绕过限制的权限
     */
    @Subscribe(order = PostOrder.FIRST)
    public void onLogin(LoginEvent event) {
        Player player = event.getPlayer();
        String ip = ipTracker.getPlayerIp(player);

        // 获取配置的最大玩家数量
        int maxPlayersPerIp = configManager.getMaxPlayersPerIp();

        // 检查IP地址是否超过限制
        if (ipTracker.isIpOverLimit(ip, maxPlayersPerIp)) {
            // 检查玩家是否有绕过限制的权限
            String bypassPermission = configManager.getBypassPermission();
            if (player.hasPermission(bypassPermission)) {
                if (configManager.isDebugEnabled()) {
                    logger.info("玩家 {} 拥有权限 {}，允许绕过IP限制", player.getUsername(), bypassPermission);
                }
                // 允许连接
                ipTracker.addPlayer(player);
                return;
            }

            // 获取踢出消息
            String kickMessage = configManager.getKickMessage()
                    .replace("{max}", String.valueOf(maxPlayersPerIp));

            // 拒绝连接
            event.setResult(ResultedEvent.ComponentResult.denied(
                    net.kyori.adventure.text.Component.text(kickMessage)));

            if (configManager.isDebugEnabled()) {
                logger.info("拒绝玩家 {} 的连接请求，IP {} 已达到最大在线玩家数量 {}",
                        player.getUsername(), ip, maxPlayersPerIp);
            }
        } else {
            // 添加玩家到IP跟踪器
            ipTracker.addPlayer(player);
        }
    }

    /**
     * 监听玩家断开连接事件
     */
    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        // 从IP跟踪器中移除玩家
        ipTracker.removePlayer(event.getPlayer());
    }
}
