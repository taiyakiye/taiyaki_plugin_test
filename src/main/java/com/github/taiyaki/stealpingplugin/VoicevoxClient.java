package com.github.taiyaki.stealpingplugin;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public class VoicevoxClient{
	private static final String BASE_URL = "http://localhost:50021";
	
	/**
	 * テキストからWAV音声ファイルを生成する
	 * @param text しゃべらせたいテキスト
	 * @param speakerId 話者id (例: 3=ずんだもん ノーマル)
	 * @param outputPath 保存先のファイルパス(.wav)
	 */
	public static void generateAudio(String text, int speakerId, Path outputPath) throws Exception {
		HttpClient client = HttpClient.newHttpClient();
		
		// audio_query にPOSTし、音声パラメータ(JSON)を取得
		String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
		String queryUrl = String.format("%s/audio_query?text=%s&speaker=%d", BASE_URL, encodedText, speakerId);
		
		HttpRequest queryRequest = HttpRequest.newBuilder()
				.uri(URI.create(queryUrl))
				.POST(HttpRequest.BodyPublishers.noBody())
				.build();
		
		HttpResponse<String> queryResponse = client.send(queryRequest, HttpResponse.BodyHandlers.ofString());
		if(queryResponse.statusCode() != 200) {
			throw new RuntimeException("AUdioQueryの取得に失敗しました: " + queryResponse.statusCode());
		}
		String audioQueryJson = queryResponse.body();
		
		// /synthesis にJSONをPOSTし、.wavバイナリを取得して保存
		String synthUrl = String.format("%s/synthesis?speaker=%d", BASE_URL, speakerId);
		
		HttpRequest synthRequest = HttpRequest.newBuilder()
				.uri(URI.create(synthUrl))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(audioQueryJson))
				.build();
		
		// ファイルとしてとしてそのまま書き出し
		client.send(synthRequest, HttpResponse.BodyHandlers.ofFile(outputPath));
	}
}