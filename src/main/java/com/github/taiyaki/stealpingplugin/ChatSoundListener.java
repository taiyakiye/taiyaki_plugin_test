package com.github.taiyaki.stealpingplugin;

import io.papermc.paper.event.player.AsyncChatEvent;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

public class ChatSoundListener implements Listener {
	private final JavaPlugin plugin;
	
	private final File dataFile;
	private int speakerId=0; // デフォルト値

	public ChatSoundListener(JavaPlugin plugin) {
		this.plugin = plugin;
		
		// plugins/StealPingPlugin/speaker_data.txtのファイルパスを設定
		this.dataFile= new File(plugin.getDataFolder(),"speaker_id.txt");
		
		// 初期化時にファイルからメモリへ読み込む
		loadSpeakerId();
	}
	
	// ファイルから話者データを読み込み
	public synchronized void loadSpeakerId() {
		if(!plugin.getDataFolder().exists()) {
			plugin.getDataFolder().mkdirs();
		}
		
		if (!dataFile.exists()) {
            // ファイルが存在しない場合は初期作成（デフォルトの 8 を書き込む）
            try {
                if (!plugin.getDataFolder().exists()) {
                    plugin.getDataFolder().mkdirs();
                }
                Files.writeString(dataFile.toPath(), "0", StandardCharsets.UTF_8);
            } catch (IOException e) {
                plugin.getLogger().warning("speaker_id.txt の自動生成に失敗しました: " + e.getMessage());
            }
            return;
        }
		
		try {
			// ファイル全体を文字列として読み込み、前後の空白や改行を trim() で除去
			String content = Files.readString(dataFile.toPath(), StandardCharsets.UTF_8).trim();
			
			if(content.isEmpty()) {
				// 文字列を数値(int)に変換
				this.speakerId = Integer.parseInt(content);
				plugin.getLogger().info("speakerId を読み込みました: " + this.speakerId);
			}
		} catch(NumberFormatException e) {
			plugin.getLogger().severe("speaker_id.txt に数値以外の文字が含まれています。デフォルト(0)を使用します。");
		} catch(IOException e) {
				plugin.getLogger().severe("話者データの読み込みエラー: "+e.getMessage());
		}
	}
	
	public int getSpeakerId() {
		return this.speakerId;
	}
	
	@EventHandler
	public void onPlayerChat(AsyncChatEvent event) {
		Player player = event.getPlayer();
		// チャット本文を平文テキストに変換
		String message = PlainTextComponentSerializer.plainText().serialize(event.message());
		String playerUuid = event.getPlayer().getUniqueId().toString();
		
		// 非同期スレッドでVOICEVOX APIを呼び出し
		plugin.getServer().getScheduler().runTaskAsynchronously(plugin,()->{
			try {
				// 保存先: plugins/StealPingPlugin/voices/latest.wav
				File voiceFolder = new File(plugin.getDataFolder(), "voices");
				if(!voiceFolder.exists()) voiceFolder.mkdirs();
				
				Path outputPath = new File(voiceFolder, playerUuid + ".wav").toPath();
				
				// VOICEVOXで音声生成(例 スピーカーID 3 = ずんだもん)
				VoicevoxClient.generateAudio(message, speakerId, outputPath);
				plugin.getLogger().info(event.getPlayer().getName() + " のチャット音声を生成しました。");
				
				// Simple Voice Chat API 経由でゲーム内の頭上から再生
				VoicechatController.playWavToPlayer(outputPath, player);
				
			} catch (Exception e) {
				plugin.getLogger().log(Level.SEVERE, "VOICEVOX生成エラーの詳細:", e);
			}
		});
	}
}