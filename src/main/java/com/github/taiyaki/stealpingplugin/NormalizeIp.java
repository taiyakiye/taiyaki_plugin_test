package com.github.taiyaki.stealpingplugin;

import java.net.Inet6Address;
import java.net.InetAddress;

public class NormalizeIp {
	public static String run(String rawIP) {
		
		try {
			InetAddress address = InetAddress.getByName(rawIP);
			
			// 1. ループバックアドレス(Pv6の::1や0:0:..:1)なら"127.0.0.1"に統一
			if(address.isLoopbackAddress()) {
				return "127.0.0.1";
			}
			
			// 2. IPv6の中にIPv4が埋め込まれている形式(::ffff:192.168.1.10など)の解除
			if(address instanceof Inet6Address) {
				byte[] bytes = address.getAddress();
				
				// IPv4射影アドレスか判定(先頭10バイトが0,次2バイトがFF)
				boolean isIPv4Mapped = true;
				for(int i=0;i<10;i++) {
					if(bytes[i]!=0) {
						isIPv4Mapped = false;
						break;
					}
				}
				
				if(isIPv4Mapped && bytes[10] == (byte)0xFF && bytes[11] == (byte)0xFF) {
					// 末尾4バイトを取り出してIPv4アドレスとして再生成
					byte[] ipv4 = new byte[4];
					System.arraycopy(bytes, 12, ipv4, 0, 4);
					return InetAddress.getByAddress(ipv4).getHostAddress();
				}
			}
			
			return address.getHostAddress();
		} catch(Exception e){
			// 変換に失敗した場合は念のためトリムした生文字列を返す
			return rawIP.trim();
		}
	}
}
