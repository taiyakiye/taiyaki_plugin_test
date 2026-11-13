package com.github.taiyaki.stealpingplugin;

import org.bukkit.Bukkit;
import org.bukkit.Material;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import org.bukkit.inventory.meta.ItemMeta;

public class BinderHoldListener implements Listener{
	private final StealPingPlugin plugin;
	
	public BinderHoldListener(StealPingPlugin plugin) {
		this.plugin = plugin;
		plugin.getLogger().info("[DEBUG]");
	}
	
	@EventHandler
	public void onItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		
		// 持ったアイテムが本(1001)なら、1度だけ開くアニメーションを再生
		ItemStack newItem = player.getInventory().getItem(event.getNewSlot());
		if(isBinderBook(newItem)) {
			int newSlot = event.getNewSlot();
			
			// 念のため最初は閉じた状態(1001)にする
			setModelData(newItem, 1001);
			
			// アニメーションの設定
			int startModel = 1001;
			int endModel = 1004; // 例えば 1002〜1005 の4段階（3回変化）の場合
			int steps = endModel - startModel; // 変化する回数 (4回)

			// 20 ticks (1秒) を変化回数で割って、1コマあたりのTick数を自動算出 (20 / 4 = 5 ticks)
			long interval = Math.max(1L, 20L / steps);

			for (int i = 1; i <= steps; i++) {
			    final int targetModel = startModel + i;
			    long delay = i * interval; // 5L, 10L, 15L, 20L

			    Bukkit.getScheduler().runTaskLater(plugin, () -> {
			        if (player.getInventory().getHeldItemSlot() == newSlot) {
			            ItemStack current = player.getInventory().getItem(newSlot);
			            if (isBinderBook(current)) {
			                setModelData(current, targetModel);
			            }
			        }
			    }, delay);
			}
		}
	}
	
	// バインダー(1001～1003)かどうかを判定
	private boolean isBinderBook(ItemStack item) {
		if(item==null || item.getType() != Material.FEATHER || !item.hasItemMeta()) {
			return false;
		}
		ItemMeta meta = item.getItemMeta();
		if(!meta.hasCustomModelData()) {
			return false;
		}
		int cmd = meta.getCustomModelData();
		return cmd >= 1001 && cmd <= 1004;
	}
	
	// CustomModelDataを書き換えるヘルパー
	private void setModelData(ItemStack item, int data) {
		ItemMeta meta = item.getItemMeta();
		if(meta != null) {
			meta.setCustomModelData(data);
			item.setItemMeta(meta);
		}
	}
}