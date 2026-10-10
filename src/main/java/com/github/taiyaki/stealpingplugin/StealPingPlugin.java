package com.github.taiyaki.stealpingplugin;

import java.util.concurrent.ThreadLocalRandom;

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
    	otpServer.start(8080, value->{
    		
    	});
    	
        getLogger().info("StealPingPlugin (ProtocolLib Hello World) を開始します。");
        
        // ProtocolLib のマネージャーを取得
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();

        // サーバーリスト更新（SERVER_INFO パケット）を監視するリスナーを登録
        protocolManager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Status.Server.SERVER_INFO
        ) {
        	@Override
            public void onPacketSending(PacketEvent event) {
                getLogger().info("[ProtocolLib] SERVER_INFO パケットを検知しました！");

                // 1. パケットから Ping データを取得
                WrappedServerPing ping = event.getPacket().getServerPings().read(0);

                // 乱数を生成
                int randomInt = ThreadLocalRandom.current().nextInt(1, 100000000);
                
                // 2. MiniMessage でリッチな Component を作成
                MiniMessage mm = MiniMessage.miniMessage();
                Component motd = mm.deserialize(
                	String.format("%d\n" +
                    "<rainbow>Hello World from ProtocolLib!</rainbow>",randomInt)
                );

                // 3. Adventure の Component を JSON に変換し、ProtocolLib 用コンポーネントとして設定
                String jsonMotd = GsonComponentSerializer.gson().serialize(motd);
                ping.setMotD(WrappedChatComponent.fromJson(jsonMotd));
                
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