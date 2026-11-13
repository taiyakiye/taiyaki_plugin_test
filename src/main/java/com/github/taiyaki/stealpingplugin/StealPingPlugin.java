package com.github.taiyaki.stealpingplugin;


import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;

import javax.imageio.ImageIO;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
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
import com.github.taiyaki.stealpingplugin.old.ChatSoundListener;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;

public final class StealPingPlugin extends JavaPlugin implements CommandExecutor{
	// サーバー直下の icons/locked/alert.png を指定
	File iconFile = new File("icons/icon.png");
	File unknownIconFile = new File("icons/unknown_server.png");
	
	// 事前にキャッシュする用のフィールド
	private CompressedImage cachedAllowedFavicon;
	private CompressedImage cachedDeniedFavicon;
	private WrappedChatComponent cachedAllowedMotd;
	private WrappedChatComponent cachedDeniedMotd;
	
	private IpWhitelistManager ipManager;
	//private ChatSoundListener chatSoundListener;
	private OnHttpServer otpServer;
	
	@Override
    public void onEnable() {
		// コマンド処理を担当するクラスとして自身(this)を登録
		if(getCommand("stealping") != null) {
			getCommand("stealping").setExecutor(this);
		}
		if(getCommand("b") != null) {
			getCommand("b").setExecutor(new BinderCommand(this));
		}
		
		
		// 1. IP管理、HTTPさーバー初期化
		// -プラグイン自身で8080番ポートの受信用サーバーを起動
		this.ipManager = new IpWhitelistManager(getDataFolder(), getLogger());
		//this.chatSoundListener = new ChatSoundListener(this);
		
		this.otpServer = new OnHttpServer(this.ipManager);
		this.otpServer.start(8080);
	
		// 2. イベントリスナー登録
		getServer().getPluginManager().registerEvents(
				new LoginListener(this.ipManager),
				this
			);
		getServer().getPluginManager().registerEvents(
				new ChatSoundListener(this), 
				this
				);
		
		/**
		 * // 2. Simple Voice Chat APIのサービス登録
		BukkitVoicechatService service = getServer().getServicesManager().load(BukkitVoicechatService.class);
		if(service != null) {
			// VoicechatController インスタンスをSVCへ登録
			service.registerPlugin(new VoicechatController());
			getLogger().info("Simple Voice Chat へのプラグイン登録が完了しました。");
		} else {
			getLogger().warning("Simple Voice Chat プラグインが見つからないか、有効化されていません。");
		}
		 */
		
		// 3. 画像およびMOTDの事前キャッシュ(ping時のディスクアクセス、変換処理を排除)
		loadCache();
		
		getLogger().info("StealPingPlugin が正常に有効化されました。");
		
        // 4. ProtocolLib のマネージャーを取得
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
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
                
                // 登録済みIPか判定
                if(ipManager.isAllowed(playerIp)) {
                	// 認証済みクライアント
                	if(cachedAllowedFavicon != null) {
                		ping.setFavicon(cachedAllowedFavicon);
                	}
                	ping.setMotD(cachedAllowedMotd);
                } else {
                	// 未認証クライアント(エラー表示、人数隠ぺい)
                	// プロトコルバージョンを「-1」等の無効値に偽装。(×表示)
                	ping.setVersionProtocol(-1);
                	// ここで空文字を指定すると「Paper 1.20.1」の文字が消えます
                	ping.setVersionName("");
                	// 念のため人数とプレイヤー一覧を非表示にする
                    ping.setPlayersOnline(0);
                    ping.setPlayersMaximum(0);
                    ping.setPlayers(java.util.Collections.emptyList());
                    
                    if(cachedDeniedFavicon != null) {
                    	ping.setFavicon(cachedDeniedFavicon);
                    }
                    ping.setMotD(cachedDeniedMotd);
                }
                // 変更した ping を書き戻す
                event.getPacket().getServerPings().write(0, ping);
            }
        });
    }
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		// コマンド引数(/stealping reloadの"reload"部分)があるか判定
		if(args.length > 0 && args[0].equalsIgnoreCase("reload")) {
			
			// 権限チェック(プレイヤー実行時の権限確認)
			if(!sender.hasPermission("stealping.admin")) {
				sender.sendMessage("§cこのコマンドを実行する権限がありません。");
				return true;
			}
			
			// 1. 標準の config.ymlを再読み込み
			reloadConfig();
			
			// 2. 独自管理しているファイル(allowed_ips.txtなど)の再読み込み
			if(this.ipManager != null) {
				this.ipManager.loadFromFile();
			}
			
			//if(this.chatSoundListener != null) {
			//	this.chatSoundListener.loadSpeakerId();
			//}
			
			// 3. アイコン等のキャッシュがある場合はそれらも再読み込み
			// loadFaviconCache();
			
			// 実行者へ完了メッセージを送信
			sender.sendMessage("§a[StealPingPlugin] 設定とデータファイルを再読み込みしました！");
			return true;
		}
		
		return false;
	}
	
	/**
	 * 起動時に一度だけ画像読み込みとテキスト変換を行う
	 */
	private void loadCache() {
		MiniMessage mm = MiniMessage.miniMessage();
		GsonComponentSerializer gson = GsonComponentSerializer.gson();
		
		// MOTDの事前構築
		Component motdAllwed = mm.deserialize("認証済み:接続可能だよ");
		this.cachedAllowedMotd = WrappedChatComponent.fromJson(gson.serialize(motdAllwed));
		
		Component motdDenied = mm.deserialize("<dark_red>サーバーに接続できません</dark_red>");
		this.cachedDeniedMotd = WrappedChatComponent.fromJson(gson.serialize(motdDenied));
		
		// 画像の事前ロード
		try {
			if(iconFile.exists()) {
				this.cachedAllowedFavicon = CompressedImage.fromPng(ImageIO.read(iconFile));
				
			} else {
				getLogger().warning(iconFile.getPath() + " が見つかりません");
			}
			
			if(unknownIconFile.exists()) {
				this.cachedDeniedFavicon = CompressedImage.fromPng(ImageIO.read(unknownIconFile));
			} else {
				getLogger().warning(unknownIconFile.getPath()+ " が見つかりません");
			}
		} catch (IOException e) {
			getLogger().severe("アイコン画像の読み込みに失敗しました: "+e.getMessage());
		}
	}
	
    @Override
    public void onDisable() {
    	if(otpServer != null) {
    		otpServer.stop();
    	}
        getLogger().info("StealPingPlugin を停止しました。");
    }
    
    public IpWhitelistManager getIpManager() {
    	return ipManager;
    }
}