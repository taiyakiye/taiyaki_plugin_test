package com.github.taiyaki.stealpingplugin;

import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
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
	private long delay;
	
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
		delay = (long)plugin.getConfig().getLong("binder.delay", 0L);
		
		this.transX = (float)rawX / 16.0f;
		this.transY = (float)rawY / 16.0f;
		this.transZ = (float)rawZ / 16.0f;
		this.scale = (float) rawScale;
		
		
		plugin.getLogger().info(String.format(
				"[Debug:BinderConfig] raw=(%.2f, %.2f, %.2f) -> scaled=(%.3f, %.3f, %.3f), scale=%.2f, delay=%d",
				rawX, rawY, rawZ, transX, transY, transZ, scale, delay
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
			
			// ItemDisplay を召喚(初期状態:閉じた本(1000番))
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
			
			// 効果音とパーティクルを出す
			// 座標の設定
			Location effectLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.9));
			
			// パーティクルの発生
			effectLoc.getWorld().spawnParticle(
					Particle.CLOUD,
					effectLoc,
					20,
					0.2, 0.2, 0.2,
					0.05
					);
			
			// デバッグ情報をチャットとコンソールに出力
			String debugMsg = String.format(
					"§e[Debug] UUID=%s | 基準座標=(%.1f, %.1f, %.1f) | ズレ=(%.2f, %.2f, %.2f) | Scale=%.2f | Delay=%d",
					display.getUniqueId().toString().substring(0, 8),
					spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(),
					transX, transY, transZ, scale, delay
					);
			player.sendMessage(debugMsg);
			plugin.getLogger().info(debugMsg);
			
			// モデルの変更(開いた本(1001番))
			int targetModel = 1001;
			
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
			    if (display.isValid()) {
			    	display.setItemStack(createBinderItem(targetModel));
			    	player.sendMessage("§7[Debug] モデル変更 -> " + targetModel);
			    } else {
			    	player.sendMessage("§c[Debug] エンティティが既に消滅しています！");
			    }
			}, delay);
			
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
