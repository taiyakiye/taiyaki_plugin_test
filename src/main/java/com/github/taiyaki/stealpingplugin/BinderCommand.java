package com.github.taiyaki.stealpingplugin;

import com.github.taiyaki.stealpingplugin.entities.Binder;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
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
import org.bukkit.scheduler.BukkitRunnable;
import org.joml.AxisAngle4f;
import org.joml.Vector3f;

public class BinderCommand implements CommandExecutor {
	private final Map<UUID, ItemDisplay> activeBinders = new HashMap<>();
	
	private final StealPingPlugin plugin;
	// 改行インデント付きで見やすく出力するGsonインスタンス
	private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
	
	// 読み込んだデータを保持しておく変数
	private Binder data;
	//  メモリ上に保持しておく変数
	private float scale;
	private long delay;
	
	private Binder.playSound soundConfig;
	private Binder.spawnParticle particleConfig;
	private Binder.Translation translation;
	
	// Transformationの保持をする変数
	private Transformation cachedInitialTransform;
	private ItemStack[] cachedModels;
	
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
			}
			
			// Jsonファイルを読み込んでBinderクラスに変換する
			try(FileReader reader = new FileReader(jsonFile, StandardCharsets.UTF_8)){
				this.data = gson.fromJson(reader, Binder.class);
				if(this.data == null) this.data = new Binder();
				
				// playSOund　の集まりを1つの変数にくくって取り出す
				this.soundConfig = data.playsound;
				this.particleConfig = data.spawnParticle;
				this.translation = data.translation;
				
				this.scale = (float)data.scale;
				this.delay = (long)data.delay;

				plugin.getLogger().info(String.format(
						"[Debug:BinderConfig] raw=(%.2f, %.2f, %.2f) -> scaled=(%.3f, %.3f, %.3f), scale=%.2f, delay=%d",
						translation.x, translation.y, translation.z, translation.x/16f, translation.y/16f, translation.z/16f, scale, delay
						));
				
				// translationを16で割っておく
				this.translation.x /= 16f;
				this.translation.y /= 16f;
				this.translation.z /= 16f;
			}
			
			// Transformationの初期化
			this.cachedInitialTransform = new Transformation(
					new Vector3f((float)translation.x,(float)translation.y,(float)translation.z),
					new AxisAngle4f(),						// leftRotation(回転なし)
					new Vector3f(scale, scale, scale),		// scale
					new AxisAngle4f()						// rightRotation(回転なし)
					);
			
			// ItemStack(1000,1001)もあらかじめ生成してキャッシュしておく。
			this.cachedModels = new ItemStack[2];
			for(int i=0; i<2; i++) {
				this.cachedModels[i] = createBinderItem(1000+i);
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
		
		UUID uuid = player.getUniqueId();
		
		 // すでにバインダーを召喚中なら消去して終了(トグル動作？)
		ItemDisplay existing = activeBinders.get(uuid);
		if(existing != null && existing.isValid()) {
			existing.remove();
			activeBinders.remove(uuid);
			return true;
		}
		
		try {
			// 設定の使いまわし
			Location spawnLoc = getBinderTargetLocation(player);
			
			// ItemDisplay を召喚(初期状態:閉じた本(1000番))
			ItemDisplay display = player.getWorld().spawn(spawnLoc, ItemDisplay.class, entity -> {
				entity.setItemStack(cachedModels[0]);
				entity.setBillboard(Billboard.FIXED);
				entity.setTransformation(cachedInitialTransform);
			});
			
			// 召喚したエンティティをMapに登録
			activeBinders.put(uuid, display);
			
			// 効果音とパーティクルを出す
			// 座標の設定
			Location effectLoc = player.getEyeLocation().add(player.getLocation().getDirection().multiply(0.9));
			
			// playsound
			player.getWorld().playSound(
				   effectLoc,                    // 音を鳴らす座標 (Location)
				   soundConfig.sound, 			 // 音の種類 (Sound enum)
				   soundConfig.volume,           // 音量 (1.0f = 標準)
				   soundConfig.pitch             // ピッチ (0.5f〜2.0f / 低いほど重低音)
			);
			
			// パーティクルの発生
			player.getWorld().spawnParticle(
					Particle.valueOf(particleConfig.particle), 		// パーティクルの種類 (Particle enum)
				    effectLoc,      								// 発生させる座標 (Location)
				    particleConfig.count,           				// 粒の数
				    particleConfig.delta.x,							// X, Y, Z の散らばり範囲（ブロック単位）
				    particleConfig.delta.y,
				    particleConfig.delta.z,
				    particleConfig.speed            				// 粒が飛び散るスピード
				);
			
			// デバッグ情報をチャットとコンソールに出力
			String debugMsg = String.format(
					"§e[Debug] UUID=%s | 基準座標=(%.1f, %.1f, %.1f) | ズレ=(%.2f, %.2f, %.2f) | Scale=%.2f | Delay=%d",
					display.getUniqueId().toString().substring(0, 8),
					spawnLoc.getX(), spawnLoc.getY(), spawnLoc.getZ(),
					translation.x, translation.y, translation.z, scale, delay
					);
			player.sendMessage(debugMsg);
			plugin.getLogger().info(debugMsg);
			
			Bukkit.getScheduler().runTaskLater(plugin, () -> {
			    if (display.isValid()) {
			    	display.setItemStack(cachedModels[1]);
			    	player.sendMessage("§7[Debug] モデル変更 -> " + 1001);
			    } else {
			    	player.sendMessage("§c[Debug] エンティティが既に消滅しています！");
			    }
			}, delay);
			
			 // 召喚後ずっとプレイヤーを追跡する
			new BukkitRunnable() {
				@Override
				public void run() {
					// バインダーが消えた、またはプレイヤーがログアウトしたら追跡を終了
					if(!display.isValid() || !player.isOnline()) {
						if(display.isValid()) {
							display.remove();
						}
						
						this.cancel();
						return;
					}
					display.teleport(getBinderTargetLocation(player));
				}
			}.runTaskTimer(plugin, 1L, 2L); // コマンド終了後も2tickごとにずっと動き続ける
			
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
	
	private Location getBinderTargetLocation(Player player) {
		Location loc = player.getEyeLocation();
		loc.setPitch(player.getLocation().getPitch() + 30f);
		return loc;
	}
}
