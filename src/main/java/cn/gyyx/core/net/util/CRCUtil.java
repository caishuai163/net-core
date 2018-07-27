package cn.gyyx.core.net.util;

import java.util.zip.CRC32;

public class CRCUtil {

	public static long Generic(byte[] bytes) {
		
		CRC32 crc32 = new CRC32();
		
		crc32.update(bytes);
		
		return crc32.getValue();
	}
}
