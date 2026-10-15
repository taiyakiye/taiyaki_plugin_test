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
	
	// 認証済み IP を保持するスレッドセーフな set??
	private final Set<String> allowedIps = ConcurrentHashMap.newKeySet();
	
	public void start(int port) {
		try {
			// 0.0.0.0
			server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
			executor = Executors.newFixedThreadPool(2);
			server.setExecutor(executor);
			
			// /unlock へのアクセスを待ち受ける
			server.createContext("/unlock", exchange->{
				try {
					if("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
						// 通信してきたクライアントのIPアドレスを取得
						String rawIp = exchange.getRemoteAddress().getAddress().getHostAddress();
						
						// IPv4へ正規化
						String clientIp = NormalizeIp.run(rawIp);
						
						// IPを登録
						allowedIps.add(clientIp);
						
						byte[] res = ("IPを登録: "+clientIp).getBytes(StandardCharsets.UTF_8);
						exchange.sendResponseHeaders(200, res.length);
						try(OutputStream os = exchange.getResponseBody()){
							os.write(res);
						}
					} else {
						exchange.sendResponseHeaders(405,-1);
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
					// DELETEでもPOSTでも受け付けられるようにする
					if("DELETE".equalsIgnoreCase(method) || "POST".equalsIgnoreCase(method)) {
						String rawIp = exchange.getRemoteAddress().getAddress().getHostAddress();
						String clientIp = NormalizeIp.run(rawIp);
						
						// リストから削除
						boolean removed = allowedIps.remove(clientIp);
						
						String msg;
						if(removed) {
							msg = "IPは正常に消えたよ: " + clientIp;
						} else {
							msg = "whiteListにIPはなかったよ: " + clientIp;
						}
						
						byte[] res = msg.getBytes(StandardCharsets.UTF_8);
						exchange.sendResponseHeaders(200, res.length);
						try(OutputStream os = exchange.getResponseBody()){
							os.write(res);
						}
					} else {
						exchange.sendResponseHeaders(405, -1);
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
	
	// IPが登録されているかチェック
	public boolean isAllowed(String ip) {
		return allowedIps.contains(ip);
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
