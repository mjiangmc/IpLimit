package newblock.ipLimit.command;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import newblock.ipLimit.config.ConfigManager;
import newblock.ipLimit.tracker.IpTracker;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * 处理IP限制插件的命令
 */
public class IpLimitCommand implements SimpleCommand {
    private final ConfigManager configManager;
    private final IpTracker ipTracker;

    public IpLimitCommand(ConfigManager configManager, IpTracker ipTracker) {
        this.configManager = configManager;
        this.ipTracker = ipTracker;
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource source = invocation.source();
        String[] args = invocation.arguments();

        if (args.length == 0) {
            showHelp(source);
            return;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                if (!source.hasPermission("iplimit.admin")) {
                    source.sendMessage(Component.text("你没有权限执行此命令！").color(NamedTextColor.RED));
                    return;
                }
                configManager.reloadConfig();
                ipTracker.setDebug(configManager.isDebugEnabled());
                source.sendMessage(Component.text("配置已重新加载！").color(NamedTextColor.GREEN));
                break;
            case "status":
                if (!source.hasPermission("iplimit.admin")) {
                    source.sendMessage(Component.text("你没有权限执行此命令！").color(NamedTextColor.RED));
                    return;
                }
                showStatus(source);
                break;
            case "list":
                if (!source.hasPermission("iplimit.admin")) {
                    source.sendMessage(Component.text("你没有权限执行此命令！").color(NamedTextColor.RED));
                    return;
                }
                if (args.length > 1) {
                    showIpPlayers(source, args[1]);
                } else {
                    showAllIps(source);
                }
                break;
            default:
                showHelp(source);
                break;
        }
    }

    /**
     * 显示帮助信息
     */
    private void showHelp(CommandSource source) {
        source.sendMessage(Component.text("===== IpLimit 帮助 =====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("/iplimit reload - 重新加载配置").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/iplimit status - 显示当前IP限制状态").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/iplimit list - 显示所有IP地址及其在线玩家数量").color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("/iplimit list <ip> - 显示指定IP地址的所有在线玩家").color(NamedTextColor.YELLOW));
    }

    /**
     * 显示当前IP限制状态
     */
    private void showStatus(CommandSource source) {
        source.sendMessage(Component.text("===== IpLimit 状态 =====").color(NamedTextColor.GOLD));
        source.sendMessage(Component.text("每个IP地址允许的最大在线玩家数量: " + configManager.getMaxPlayersPerIp()).color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("绕过IP限制的权限: " + configManager.getBypassPermission()).color(NamedTextColor.YELLOW));
        source.sendMessage(Component.text("调试模式: " + (configManager.isDebugEnabled() ? "开启" : "关闭")).color(NamedTextColor.YELLOW));
    }

    /**
     * 显示所有IP地址及其在线玩家数量
     */
    private void showAllIps(CommandSource source) {
        Map<String, Integer> ipCounts = ipTracker.getAllIpCounts();

        source.sendMessage(Component.text("===== IP地址列表 =====").color(NamedTextColor.GOLD));
        if (ipCounts.isEmpty()) {
            source.sendMessage(Component.text("当前没有在线玩家").color(NamedTextColor.YELLOW));
            return;
        }

        for (Map.Entry<String, Integer> entry : ipCounts.entrySet()) {
            source.sendMessage(Component.text(entry.getKey() + ": " + entry.getValue() + " 个玩家").color(NamedTextColor.YELLOW));
        }
    }

    /**
     * 显示指定IP地址的所有在线玩家
     */
    private void showIpPlayers(CommandSource source, String ip) {
        Set<String> players = ipTracker.getPlayersByIp(ip);

        source.sendMessage(Component.text("===== IP " + ip + " 的在线玩家 =====").color(NamedTextColor.GOLD));
        if (players.isEmpty()) {
            source.sendMessage(Component.text("该IP地址没有在线玩家").color(NamedTextColor.YELLOW));
            return;
        }

        for (String player : players) {
            source.sendMessage(Component.text(player).color(NamedTextColor.YELLOW));
        }
    }

    @Override
    public CompletableFuture<List<String>> suggestAsync(Invocation invocation) {
        String[] args = invocation.arguments();
        List<String> suggestions = new ArrayList<>();

        if (args.length == 0 || args.length == 1) {
            suggestions.add("reload");
            suggestions.add("status");
            suggestions.add("list");
        } else if (args.length == 2 && args[0].equalsIgnoreCase("list")) {
            // 添加所有IP地址作为建议
            suggestions.addAll(ipTracker.getAllIpCounts().keySet());
        }

        return CompletableFuture.completedFuture(suggestions);
    }

    @Override
    public boolean hasPermission(Invocation invocation) {
        return invocation.source().hasPermission("iplimit.admin");
    }
}
