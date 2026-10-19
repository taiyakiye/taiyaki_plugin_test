package com.github.taiyaki.stealpingplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.server.ServerListPingEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class LoginListener implements Listener {
	// 1. フィールドをIpWhitelistManager に変更
	private final IpWhitelistManager ipManager;
	
	// 2. 引数にIpWhitelistManager を受け取るコンストラクタを定義する
	public LoginListener(IpWhitelistManager ipManager) {
		this.ipManager = ipManager;
	}
	// サーバーリスト表示の制御(ServerListPingEventを使う場合)
	@EventHandler
	public void onServerListPing(ServerListPingEvent event) {
		String rawIp = event.getAddress().getHostAddress();
		String playerIp = NormalizeIp.run(rawIp);
		
		if(!ipManager.isAllowed(playerIp)) {
			event.setMaxPlayers(0);
		}
	}
	
	// 参加直前のキック処理
	@EventHandler
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		String rawIp = event.getAddress().getHostAddress();
		String playerIp = NormalizeIp.run(rawIp);
		
		if (!ipManager.isAllowed(playerIp)) {
			// MiniMessageを使ってComponentを作成
			Component kickReason = MiniMessage.miniMessage().deserialize(
					"サーバーに接続できませんでした"
					);
			// Component を受け取るメソッドを呼び出す
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, 
                kickReason
            );
        }
	}
}
