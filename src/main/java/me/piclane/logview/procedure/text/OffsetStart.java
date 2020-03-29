package me.piclane.logview.procedure.text;

/**
 * 読込の開始位置のモード
 */
public enum OffsetStart {
    /** 指定されたオフセットがファイル先頭からの相対位置になる */
    head,

    /** 指定されたオフセットがファイル末尾からの相対位置になる */
    tail
}
