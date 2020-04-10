package me.piclane.logview.procedure;

class Line {
    /** 行頭の位置 */
    public final long pos;

    /** バイト長 */
    public final long len;

    /** 一行の文字列 */
    public final String str;

    /**
     * コンストラクタ
     *
     * @param pos 行頭の位置
     * @param len バイト長
     * @param string 一行の文字列
     */
    public Line(long pos, long len, String string) {
        this.pos = pos;
        this.len = len;
        this.str = string;
    }

    /**
     * @see Object#toString()
     */
    @Override
    public String toString() {
        return str;
    }
}
