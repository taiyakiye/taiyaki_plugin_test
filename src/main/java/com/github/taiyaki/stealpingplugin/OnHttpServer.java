package com.github.taiyaki.stealpingplugin;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.sun.net.httpserver.HttpServer;

public class OnHttpServer {
	private HttpServer server;
	private ExecutorService executor;
	private final IpWhitelistManager ipManager;
	
	public OnHttpServer(IpWhitelistManager ipManager) {
		this.ipManager = ipManager;
	}
	
	public void start(int port) {
		try {
			// 0.0.0.0
			server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
			executor = Executors.newFixedThreadPool(2);
			server.setExecutor(executor);
			
			/* ======================================
			 * 
			 * 1. /unlock (登録)
			 * DELETEメソッド、またはPOSTで受け付ける
			 * ======================================
			 */
			server.createContext("/unlock", exchange->{
				try {
					if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
						exchange.sendResponseHeaders(405,-1);
						return;
					}
					
					// 通信してきたクライアントのIPアドレスを取得
					String rawIp = exchange.getRemoteAddress().getAddress().getHostAddress();
					// IPv4へ正規化
					String clientIp = NormalizeIp.run(rawIp);
					
					// Manager を使って追加
					ipManager.addIp(clientIp);
					
					String msg = "IP を登録しました: "+ clientIp;
					byte[] res = msg.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(200,res.length);
					try(OutputStream os = exchange.getResponseBody()){
						os.write(res);
					}
				} finally {
					exchange.close();
				}
			});
			
			/* ======================================
			 * 
			 * 2. IP削除用(/lock) 
			 * DELETEメソッド、またはPOSTで受け付ける
			 * ======================================
			 */
			server.createContext("/lock",exchange->{
				try {
					String method = exchange.getRequestMethod();
					if(!"DELETE".equalsIgnoreCase(method) && !"POST".equalsIgnoreCase(method)) {
						exchange.sendResponseHeaders(405,-1);
						return;
					}
					
					String rawIp = exchange.getRemoteAddress().getAddress().getHostAddress();
					String clientIp = NormalizeIp.run(rawIp);
					
					// Managerを使って削除
					boolean removed = ipManager.removeIp(clientIp);
					
					String msg = removed ? "IP を削除しました: " + clientIp : "IP が見つかりませんでした: " + clientIp;
					byte[] res = msg.getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(200,res.length);
					try(OutputStream os = exchange.getResponseBody()){
						os.write(res);
					}
				} finally {
					exchange.close();
				}
			});
			
			server.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void stop() {
		if(server != null) {
			server.stop(0);
		}
		if(executor != null) {
			executor.shutdown();
		}
	}
}
