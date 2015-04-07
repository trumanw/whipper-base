package utils

import org.xerial.snappy.Snappy
// import java.util.Base64	// Only available in Java 8
import org.apache.commons.codec.binary.Base64;

object Snoopy {
	def decomp(src: Option[String]): Option[String] = {
		if (src.isDefined) {
			val bytes = Snappy.uncompress(Base64.decodeBase64(src.get))
			val ret = new String(bytes, "UTF-8")
			Option(ret)
		} else {
			None
		}
	}

	def comp(src: Option[String]): Option[String] = {
		if (src.isDefined) {
			val bytes = Snappy.compress(src.get.getBytes("UTF-8"))
			val ret = Base64.encodeBase64String(bytes)
			Option(ret)
		} else {
			None
		}
	}
}