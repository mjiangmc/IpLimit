package newblock.ipLimit.tracker;

import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import org.slf4j.Logger;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * 跟踪每个IP地址的在线玩家数量
 */
public class IpTracker {
    private final ProxyServer server;
    private final Logger logger;
    private final Map<String, Set<String>> ipToPlayers;
    private boolean debug;

    public IpTracker(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
        this.ipToPlayers = new HashMap<>();
        this.debug = false;
    }

    /**
     * 设置调试模式
     */
    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * 添加玩家到IP跟踪器
     */
    public void addPlayer(Player player) {
        String ip = getPlayerIp(player);
        String playerName = player.getUsername();

        if (ip == null) {
            if (debug) {
                logger.warn("无法获取玩家 {} 的IP地址", playerName);
            }
            return;
        }

        ipToPlayers.computeIfAbsent(ip, k -> new HashSet<>()).add(playerName);

        if (debug) {
            logger.info("玩家 {} 已连接，IP: {}，当前该IP在线玩家数: {}",
                    playerName, ip, getPlayerCountByIp(ip));
        }
    }

    /**
     * 从IP跟踪器中移除玩家
     */
    public void removePlayer(Player player) {
        String ip = getPlayerIp(player);
        String playerName = player.getUsername();

        if (ip == null) {
            return;
        }

        Set<String> players = ipToPlayers.get(ip);
        if (players != null) {
            players.remove(playerName);

            if (players.isEmpty()) {
                ipToPlayers.remove(ip);
            }

            if (debug) {
                logger.info("玩家 {} 已断开连接，IP: {}，当前该IP在线玩家数: {}",
                        playerName, ip, getPlayerCountByIp(ip));
            }
        }
    }

    /**
     * 获取指定IP地址的在线玩家数量
     */
    public int getPlayerCountByIp(String ip) {
        Set<String> players = ipToPlayers.get(ip);
        return players != null ? players.size() : 0;
    }

    /**
     * 获取玩家的IP地址
     */
    public String getPlayerIp(Player player) {
        InetSocketAddress address = player.getRemoteAddress();
        if (address == null) {
            return null;
        }

        // 获取IP地址，去除端口号
        String hostAddress = address.getAddress().getHostAddress();

        // 处理IPv6地址
        if (hostAddress.contains(":")) {
            // 简化IPv6地址，只保留前两段和最后两段
            String[] parts = hostAddress.split(":");
            if (parts.length > 4) {
                hostAddress = parts[0] + ":" + parts[1] + ":..." + parts[parts.length - 2] + ":" + parts[parts.length - 1];
            }
        }

        return hostAddress;
    }

    /**
     * 检查IP地址是否超过限制
     */
    public boolean isIpOverLimit(String ip, int maxPlayersPerIp) {
        int currentCount = getPlayerCountByIp(ip);
        return currentCount >= maxPlayersPerIp;
    }

    /**
     * 获取所有IP地址及其在线玩家数量的映射
     */
    public Map<String, Integer> getAllIpCounts() {
        Map<String, Integer> result = new HashMap<>();
        for (Map.Entry<String, Set<String>> entry : ipToPlayers.entrySet()) {
            result.put(entry.getKey(), entry.getValue().size());
        }
        return result;
    }

    /**
     * 获取指定IP地址的所有在线玩家名称
     */
    public Set<String> getPlayersByIp(String ip) {
        Set<String> players = ipToPlayers.get(ip);
        return players != null ? new HashSet<>(players) : new HashSet<>();
    }
}
