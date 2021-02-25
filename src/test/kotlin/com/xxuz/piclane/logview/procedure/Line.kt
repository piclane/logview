package com.xxuz.piclane.logview.procedure

internal class Line (
        /** 行頭の位置  */
        val pos: Long,

        /** バイト長  */
        val len: Long,

        /** 一行の文字列  */
        val str: String
) {
    /**
     * @see Object.toString
     */
    override fun toString(): String {
        return str
    }
}
