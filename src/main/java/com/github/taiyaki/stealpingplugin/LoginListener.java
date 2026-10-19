package com.github.taiyaki.stealpingplugin;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;


public class LoginListener implements Listener {
	private final OnHttpServer otpServer;
	
	public LoginListener(OnHttpServer otpServer) {
		this.otpServer = otpServer;
	}
	
	@EventHandler
	public void onPreLogin(AsyncPlayerPreLoginEvent event) {
		String rawIp = event.getAddress().getHostAddress();
		String playerIp = NormalizeIp.run(rawIp);
		
		if (!otpServer.isAllowed(playerIp)) {
			// MiniMessageを使ってComponentを作成
			Component kickReason = MiniMessage.miniMessage().deserialize(
					"不明なホストです"
					);
			// Component を受け取るメソッドを呼び出す
            event.disallow(
                AsyncPlayerPreLoginEvent.Result.KICK_WHITELIST, 
                kickReason
            );
        }
	}
}
