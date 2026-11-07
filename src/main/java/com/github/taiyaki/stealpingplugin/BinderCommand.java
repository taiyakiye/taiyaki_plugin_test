package com.github.taiyaki.stealpingplugin;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;

public class BinderCommand implements CommandExecutor{
	private final StealPingPlugin plugin;
	
	public BinderCommand(StealPingPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// コンソールからの実行を防止(プレイヤーのみ)
		if(!(sender instanceof Player player)) {
			sender.sendMessage("§cこのコマンドはゲーム内から実行してください。");
			return true;
		}
		
		// 1. スポーン座標の計算(プレイヤーの目線の先0.8マス)
		Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.8));
		
		// 2. ItemDisplay(バインダーの3Dモデル)を生成
		ItemDisplay binderDisplay = player.getWorld().spawn(spawnLoc, ItemDisplay.class, display -> {
			// Blockbenchモデルを割り当てた本アイテム
			ItemStack displayItem = new ItemStack(Material.WRITTEN_BOOK);
			
		});
	}
}