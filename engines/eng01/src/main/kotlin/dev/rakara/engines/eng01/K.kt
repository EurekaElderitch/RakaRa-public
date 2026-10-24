
internal object K {
    private fun hex(s: String): ByteArray {
        val out = ByteArray(s.length / 2)
        var i = 0
        while (i < s.length) {
            out[i / 2] = ((Character.digit(s[i], 16) shl 4) or Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return out
    }

    private fun dec(e: ByteArray): String {
        val k = k1 + k2 + k3
        val out = ByteArray(e.size)
        for (i in e.indices) out[i] = (e[i].toInt() xor k[i % k.size].toInt()).toByte()
        return String(out, Charsets.UTF_8)
    }

    private val k1 = hex("9486e40ab8d00bba1827")
    private val k2 = hex("ac839b2afa3828300553")
    private val k3 = hex("9d37888da5f1147ca2997a03")

    val url: String by lazy { dec(hex("fcf2907acbea24957342c5e8f447935306536a3e")) }
    val ua: String by lazy { dec(hex("d9e99e63d4bc6a952d099ca3b37d93564c5f7220bd79dcad94c13a4c99b92d6afab0d03198a83d8e3107edf3eb469f6f4d524e3ae918bdbe92df274a82b1314bc0cba82698bc62d17d07ebe6f841951108736d21f25aeda294c3245292b74a2da4a6b76bdeb179d337129fb4b519cc")) }
}
