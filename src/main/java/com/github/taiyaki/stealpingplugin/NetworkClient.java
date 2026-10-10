package com.github.taiyaki.stealpingplugin;

public class NetworkClient {
	/**
	 *  汎用的な受信リスナー関数
	 *  
	 *  @param host 接続先ホスト
	 *  @param port 接続先ポート
	 *  @param onReceive 1行受信するたびに実行する処理
	 *  @param onError エラー発生時に実行処理
	 */
	public static void startListening(
			String host,
			int port,
			Consumer<String> onReceive,
			Consumer<Exception> onError) {
		
		// 通信処理をバックグラウンドスレッドで実行
		new Thread(()-> {
			try(Socket socket = new Socket(host,port);
					BufferdReader reader = new BufferedReader(new InputStreamReader(socket.getInput))
					)
		})
	}
}
