package com.github.taiyaki.stealpingplugin.old;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.github.taiyaki.stealpingplugin.StealPingPlugin;

public class BinderHoldListener implements Listener{
	private final StealPingPlugin plugin;
	
	public BinderHoldListener(StealPingPlugin plugin) {
		this.plugin = plugin;
		plugin.getLogger().info("[DEBUG] BookHoldListener が正常に初期化されました！");
	}
	
	@EventHandler
	public void onItemHeld(PlayerItemHeldEvent event) {
		Player player = event.getPlayer();
		int newSlot = event.getNewSlot();
		ItemStack newItem = player.getInventory().getItem(newSlot);
		
		if(!isBinderBook(newItem)) {
			return;
		}

		// 持ち帰自体が検知されているか確認
		plugin.getLogger().info(String.format("[DEBUG] %s がスロット %s に持ち替えました。", player.getName(), newSlot));
		
		plugin.getLogger().info(String.format("[DEBUG] アイテム種類: %s / Meta有無: %s", newItem.getType(), newItem.hasItemMeta()));
		if(newItem.hasItemMeta() && newItem.getItemMeta().hasCustomModelData()) {
			plugin.getLogger().info(String.format("[DEBUG] CustomModelData: %s", newItem.getItemMeta().getCustomModelData()));
		} else {
			plugin.getLogger().info("[DEBUG] CustomModelData が設定されていません。");
		}
		
		
		// 1度だけ開くアニメーションを再生
		// 閉じた状態(1001)にする
		setModelData(newItem, 1001);
		plugin.getLogger().info("[DEBUG] バインダーのアニメーションを開始します。");
			
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
			    if (player.getInventory().getHeldItemSlot() == newSlot) {
			        ItemStack current = player.getInventory().getItem(newSlot);
			        if (isBinderBook(current)) {
			            setModelData(current, targetModel);
			            plugin.getLogger().info("[DEBUG] モデルを " + targetModel + " に切り替えました (delay: " + delay + "L)");
			        }
			    } else {
			    	plugin.getLogger().info("[DEBUG] 途中で別スロットに持ち替えられたため中断しました。");
			    }
			}, delay);
		}
	}
	
	// バインダー(1001～1004)かどうかを判定
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