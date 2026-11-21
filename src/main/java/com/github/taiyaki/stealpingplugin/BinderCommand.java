package com.github.taiyaki.stealpingplugin;

import com.github.taiyaki.stealpingplugin.entities.Binder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Display.Billboard;
import org.bukkit.entity.ItemDisplay;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.util.Transformation;
import org.bukkit.plugin.java.JavaPlugin;
import org.joml.Vector3f;

public class BinderCommand implements CommandExecutor {
	private final StealPingPlugin plugin;
	// 改行インデント付きで見やすく出力するGsonインスタンス
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	// 読み込んだデータを保持しておく変数
	private Binder data;
	//  メモリ上に保持しておく変数
	private float transX;
	private float transY;
	private float transZ;
	private float scale;
	private long delay;
	private String sound;
	private String particle;
	
	public BinderCommand(StealPingPlugin plugin) {
		this.plugin = plugin;
		// 起動時に1回だけ読み込む
		loadConfig();
	}
	
	// config.yml から値を読み込んで変数にセットするメソッド
	public void loadConfig() {
		File jsonFile = new File(plugin.getDataFolder(), "entities/binder.json");
		
		try {
			// ファイルがなければ初期化(new Binder()) で自動作成する
			if(!jsonFile.exists()) {
				this.data = Binder.createDefault(jsonFile);
				return;
			}
			
			// Jsonファイルを読み込んでBinderクラスに変換する
			try(FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)){
				this.data = gson.fromJson(reader, Binder.class);
				if(this.data == null) this.data = new Binder();
				

				

				// playSOund　の集まりを1津の変数にくくって取り出す
				Binder.playSound soundConfig = data.playsound;
				Binder.spawnParticle particleConfig = data.spawnParticle;
				Binder.Translation translationConfig = data.translation;
				
				this.scale = (float)data.scale;
				this.delay = (long)data.delay;
				this.transX = (float)rawX / 16.0f;
				this.transY = (float)rawY / 16.0f;
				this.transZ = (float)rawZ / 16.0f;
				

				plugin.getLogger().info(String.format(
						"[Debug:BinderConfig] raw=(%.2f, %.2f, %.2f) -> scaled=(%.3f, %.3f, %.3f), scale=%.2f, delay=%d",
						rawX, rawY, rawZ, transX, transY, transZ, scale, delay
						));
			}
		} catch(IOException e) {
			plugin.getLogger().severe("binder.json の読み込みに失敗しました: "+e.getMessage());
			this.data = new Binder();
		} 
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
			
			// playsound
			player.getWorld().playSound(
				   effectLoc,                    // 音を鳴らす座標 (Location)
				   Sound.ENTITY_GENERIC_EXPLODE, // 音の種類 (Sound enum)
				   1.0f,                         // 音量 (1.0f = 標準)
				   0.75f                         // ピッチ (0.5f〜2.0f / 低いほど重低音)
			);
			
			// パーティクルの発生
			player.getWorld().spawnParticle(
				    Particle.CLOUD, // パーティクルの種類 (Particle enum)
				    effectLoc,      // 発生させる座標 (Location)
				    20,             // 粒の数
				    0.2, 0.2, 0.2,  // X, Y, Z の散らばり範囲（ブロック単位）
				    0.05            // 粒が飛び散るスピード
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
