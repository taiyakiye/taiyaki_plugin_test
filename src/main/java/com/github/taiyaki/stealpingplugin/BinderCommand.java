package com.github.taiyaki.stealpingplugin;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.joml.Vector3f;

public class BinderCommand implements CommandExecutor {
	private final StealPingPlugin plugin;
	
	public BinderCommand(StealPingPlugin plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
            return true;
        }
		
		// プレイヤーの目のまえ(1.0先)に召喚位置を計算
		Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection());
		// プレイヤーの方を向かせる
		spawnLoc.setYaw(player.getLocation().getYaw() + 180f);
		spawnLoc.setPitch(0f);
		
		// ItemDisplay を召喚(初期状態:閉じた本(1001番))
		ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
			entity.setItemStack(createBinderItem(1001));
			entity.setBillboard(Billboard.FIXED);
			
			Transformation initial = entity.getTransformation();
			initial.getScale().set(new Vector3f(1.2f, 1.2f, 1.2f));
			entity.setTransformation(initial);
		});
		
		// アニメーションの設定
		int startModel = 1001;
		int endModel = 1004; // 例えば 1001〜1004 の4段階（3回変化）の場合
		int steps = endModel - startModel; // 変化する回数 (3回)

		// 20 ticks (1秒) を変化回数で割って、1コマあたりのTick数を自動算出 (20 / 4 = 5 ticks)
		long interval = Math.max(1L, 10L / steps);

		for (int i = 1; i <= steps; i++) {
			final int targetModel = startModel + i;
			long delay = i * interval; // 5L, 10L, 15L, 20L

			Bukkit.getScheduler().runTaskLater(plugin, () -> {
			    if (display.isValid()) {
			    	display.setItemStack(createBinderItem(targetModel));
			    }
			}, delay);
		}
		
		return true;
	}
	
	// 指定した CustomModelData を持つアイテムを生成するヘルパー
	private ItemStack createBinderItem(int modelData) {
		ItemStack item = new ItemStack(Material.FEATHER);
		ItemMeta meta = item.getItemMeta();
		if(meta != null) {
			meta.setCustomModelData(modelData);
			item.setItemMeta(meta);
		}
		return item;
	}
}
