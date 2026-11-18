package com.github.taiyaki.stealpingplugin;

import java.util.logging.Level;

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
	
	//  メモリ上に保持しておく変数
	private float transX;
	private float transY;
	private float transZ;
	private float scale;
	
	public BinderCommand(StealPingPlugin plugin) {
		this.plugin = plugin;
		// 起動時に1回だけ読み込む
		loadConfig();
	}
	
	// config.yml から値を読み込んで変数にセットするメソッド
	public void loadConfig() {
		double rawX = (float)plugin.getConfig().getDouble("binder.translation.x", 0.0);
		double rawY = (float)plugin.getConfig().getDouble("binder.translation.y", 0.0);
		double rawZ = (float)plugin.getConfig().getDouble("binder.translation.z", 0.0);
		double rawScale  = (float)plugin.getConfig().getDouble("binder.scale", 1.0);
		
		this.transX = (float)rawX / 16.0f;
		this.transY = (float)rawY / 16.0f;
		this.transZ = (float)rawZ / 16.0f;
		this.scale = (float) rawScale;
		
		plugin.getLogger().info(String.format(
				"[Debug:BinderConfig] raw=(%.2f, %.2f, %.2f) -> scaled=(%.3f, %.3f, %.3f), scale=%.2f",
				rawX, rawY, rawZ, transX, transY, transZ, scale
				));
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player player)) {
			sender.sendMessage("§c[Debug] プレイヤー以外から実行されました");
            return true;
        }
		
		try {
			// プレイヤーの目のまえ(1.0先)に召喚位置を計算
			Location spawnLoc = player.getEyeLocation().add(player.getLocation().getDirection());
			// プレイヤーの方+30度?向かせる
			spawnLoc.setPitch(player.getLocation().getPitch()+30.0f);
			
			// ItemDisplay を召喚(初期状態:閉じた本(1001番))
			ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
				entity.setItemStack(createBinderItem(1000));
				entity.setBillboard(Billboard.FIXED);
				
				Transformation initial = entity.getTransformation();
				// translationの設定
				initial.getTranslation().add(transX, transY, transZ);
				// サイズ設定
				initial.getScale().set(new Vector3f(scale, scale, scale));
				
				entity.setTransformation(initial);
			});
			
			// デバッグ情報をチャットとコンソールに出力
			String debugMsg = String.format(
					"§e[Debug] UUID=%s | 基準座標=(%.1f, %.1f, %.1f) | ズレ=(%.2f, %.2f, %.2f) | Scale=%.2f",
					display.getUniqueId().toString().substring(0, 8),
					spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(),
					transX, transY, transZ, scale
					);
			player.sendMessage(debugMsg);
			plugin.getLogger().info(debugMsg);
			
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
				    	player.sendMessage("§7[Debug] モデル変更 -> " + targetModel);
				    } else {
				    	player.sendMessage("§c[Debug] エンティティが既に消滅しています！");
				    }
				}, delay);
			}
		} catch(Exception e) {
			player.sendMessage("§c[Debug] 例外発生: %s - %s", e.getClass().getSimpleName(), e.getMessage());
			plugin.getLogger().log(Level.SEVERE, "[Debug BinderCommand 実行中にエラー:", e);
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
