package com.github.taiyaki.stealpingplugin;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.imageio.ImageIO;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedServerPing;
import com.comphenix.protocol.wrappers.WrappedServerPing.CompressedImage;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class StealPingPlugin extends JavaPlugin {
	// サーバー直下の icons/locked/alert.png を指定
	File iconFile = new File("icons/icon.png");
	File unknown_iconFile = new File("icons/unknown_server.png");
	
	private final OnHttpServer otpServer = new OnHttpServer();
	
	@Override
    public void onEnable() {
		// 1. プラグイン自身で8080番ポートの受信用サーバーを起動
		otpServer.start(8080);
		getLogger().info("受信用HTTPサーバーをポート8080で起動したよ");
		
		// リスナークラスを渡して登録
		getServer().getPluginManager().registerEvents(
				new LoginListener(this.otpServer),
				this);
		
        // 2. ProtocolLib のマネージャーを取得
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        // サーバーリスト更新（SERVER_INFO パケット）を監視するリスナーを登録
        protocolManager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Status.Server.SERVER_INFO
        ) {
        	@Override
            public void onPacketSending(PacketEvent event) {
                InetSocketAddress socketAddress = event.getPlayer().getAddress();
                if(socketAddress == null) return;
                String rawIp = socketAddress.getAddress().getHostAddress();
                String playerIp = NormalizeIp.run(rawIp);
                
                // パケットから Ping データを取得
                WrappedServerPing ping = event.getPacket().getServerPings().read(0);
                MiniMessage mm = MiniMessage.miniMessage();
                Component motd;
                
                // 画像の用意
                CompressedImage image;
                
                //getLogger().info("playerのIP: "+playerIp);
                
                // 登録済みIPか判定
                if(otpServer.isAllowed(playerIp)) {
                	//画像の表示
					try {
						image = CompressedImage.fromPng(ImageIO.read(iconFile));
						ping.setFavicon(image);
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
                	
                	motd = mm.deserialize("認証済み：接続可能だよ");
                } else {
                	// --- 未認証の人向けの表示（人数を消して赤い×をつける） ---
                	motd = mm.deserialize("<dark_red>サーバーに接続できません</dark_red>");
                	// プロトコルバージョンを「-1」等の無効値に偽装。(×表示)
                	ping.setVersionProtocol(-1);
                	// ここで空文字を指定すると「Paper 1.20.1」の文字が消えます
                	ping.setVersionName("");
                	
                	// 4. 念のため人数とプレイヤー一覧を非表示にする
                    ping.setPlayersOnline(0);
                    ping.setPlayersMaximum(0);
                    ping.setPlayers(java.util.Collections.emptyList());
                    
                    // 画像の表示
                    try {
						image = CompressedImage.fromPng(ImageIO.read(unknown_iconFile));
						ping.setFavicon(image);
					} catch (IOException e) {
						// TODO 自動生成された catch ブロック
						e.printStackTrace();
					}
                }
                
                String jsonMotd = GsonComponentSerializer.gson().serialize(motd);
                ping.setMotD(WrappedChatComponent.fromJson(jsonMotd));
                
                // 変更した ping を書き戻す
                event.getPacket().getServerPings().write(0, ping);
            }
        });
    }
	
    @Override
    public void onDisable() {
    	otpServer.stop();
        getLogger().info("StealPingPlugin を停止しました。");
    }
}