package com.github.taiyaki.stealpingplugin;


import java.net.InetSocketAddress;

import org.bukkit.plugin.java.JavaPlugin;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.wrappers.WrappedChatComponent;
import com.comphenix.protocol.wrappers.WrappedServerPing;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class StealPingPlugin extends JavaPlugin {
	private final OnHttpServer otpServer = new OnHttpServer();
	
	@Override
    public void onEnable() {
		// 1. プラグイン自身で8080番ポートの受信用サーバーを起動
		otpServer.start(8080);
		getLogger().info("受信用HTTPサーバーをポート8080で起動したよ");
		
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
                
                //getLogger().info("playerのIP: "+playerIp);
                
                // 登録済みIPか判定
                if(otpServer.isAllowed(playerIp)) {
                	motd = mm.deserialize("認証済み：接続可能だよ");
                } else {
                	motd = mm.deserialize("× 未認証: 接続できないよ");
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