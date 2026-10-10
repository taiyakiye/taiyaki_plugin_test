package com.github.taiyaki.stealpingplugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

import com.sun.net.httpserver.HttpServer;

public class OnHttpServer {
	private HttpServer server;
    
	// 事前に決めた合言葉
	private static final String SECRET_KEY = "aiueo700";
	
	// IPアドレスごとの許可有効期限を保存するマップ
	private final Map<String, Long> allowedIps = new ConcurrentHashMap<>();
	
	public void start(int port, Consumer<String> onReceive) {
		try {
			server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 0);
			server.setExecutor(Executors.newFixedThreadPool(2));
			
			server.createContext("/unlock", exchange->{
				try {
					if(!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
						exchange.sendResponseHeaders(405,-1);
						return;
					}
					
					// 1. 送信されたトークンの照合
					String token = exchange.getRequestHeaders().getFirst("X-Auth-Token");
					if(token == null || !SECRET_KEY.equals(token)) {
						exchange.sendResponseHeaders(401,-1);
						return;
					}
					
					// 2. 送信元のIPアドレスを取得
					String clientIp = exchange.getRemoteAddress().getAddress().getHostAddress();
					
					// 3. 5秒間のアクセスを許可
					long expireTime = System.currentTimeMillis() + (5 * 1000);
					allowedIps.put(clientIp, expireTime);
					
					String msg = "アクセス許可完了: " + clientIp + "5秒間有効";
					byte[] res = msg.getBytes(StandardCharsets.UTF_8);
					try(OutputStream os = exchange.getResponseBody()) {
						os.write(res);
					}
				} finally {
					exchange.close();
				}
			});
			
			server.createContext("/set-motd", exchange ->{
				if("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
					String value;
					try(InputStream is = exchange.getRequestBody()) {
						value = new String(is.readAllBytes(), StandardCharsets.UTF_8).trim();
					}
					onReceive.accept(value); // メイン側に値を通知
					
					byte[] res = "OK".getBytes(StandardCharsets.UTF_8);
					exchange.sendResponseHeaders(200, res.length);
					try(OutputStream os = exchange.getResponseBody()){
						os.write(res);
					}
				} else {
					exchange.sendResponseHeaders(405,-1);
				}
			});
			
			server.start();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	// IP が許可されているかチェックする判定メソッド
	public boolean isAllowed(String ip) {
		Long expireTime = allowedIps.get(ip);
		if(expireTime == null) {
			return false;
		}
		if(System.currentTimeMillis() > expireTime) {
			allowedIps.remove(ip); // 期限切れ
			return false;
		}
		return true;
	}
	
	public void stop() {
		if(server != null) {
			server.stop(0);
		}
	}
}
