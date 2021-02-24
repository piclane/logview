package com.xxuz.piclane.logview.procedure

import com.fasterxml.jackson.annotation.JsonInclude

/**
 * ログの行とは別に送信したい信号を表現します
 *
 * @author yohei_hina
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
class Signal {
    /** メッセージ  */
    val signal: String

    /** 値  */
    val value: Any?

    /**
     * コンストラクタ
     *
     * @param signal シグナル
     */
    private constructor(signal: String) {
        this.signal = signal
        value = null
    }

    /**
     * コンストラクタ
     *
     * @param signal シグナル
     * @param value 値
     */
    private constructor(signal: String, value: Any) {
        this.signal = signal
        this.value = value
    }

    companion object {
        /** ファイルの先頭  */
        val BOF = Signal("bof")

        /** ファイルの終端  */
        val EOF = Signal("eof")

        /** リクエストされたバッファを送信完了  */
        val EOR = Signal("eor")

        /** ファイルの長さ  */
        fun FILE_LENGTH(length: Long): Signal {
            return Signal("file_length", length)
        }

        /** 完全に停止した  */
        val STOPPED = Signal("stopped")
    }
}
