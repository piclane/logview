package com.xxuz.piclane.logview.procedure

import com.fasterxml.jackson.annotation.JsonProperty
import com.xxuz.piclane.logview.fs.Fs
import com.xxuz.piclane.logview.procedure.text.Direction
import com.xxuz.piclane.logview.procedure.text.OffsetStart
import java.nio.file.Path

/**
 * パラメーター
 *
 * @author yohei_hina
 */
class Param {
    /** プロシージャ */
    lateinit var procedure: Procedure

    /** パス */
    lateinit var path: Path

    /** ステータス */
    lateinit var status: Status

    /** 行の読込方向 */
    lateinit var direction: Direction

    /** 読み込む行数 (行) */
    var lines: Int = -1
        get() = if (field < 0) { Int.MAX_VALUE } else { field }

    /** 読込の開始位置 (byte)  */
    var offsetBytes: Long = 0L

    /** 読込の開始位置のモード */
    var offsetStart: OffsetStart = OffsetStart.head

    /**
     * オフセット位置からスキップする行数
     * 正値が与えられた場合、末尾方向にスキップします
     * 負値が与えられた場合、先頭方向にスキップします
     * 0 の場合はスキップを行いません
     */
    var skipLines: Int = 0

    /** ファイルが大きくなるのに合わせて追加されたデータを出力するかどうか */
    @JsonProperty("follow")
    var isFollow: Boolean = false
        get() = field && direction == Direction.forward

    /** 検索文字列 */
    var search: Array<String> = arrayOf()

    class ParamBuilder {
        /** プロシージャ */
        lateinit var procedure: Procedure

        /** パス */
        lateinit var path: String

        /** ステータス */
        lateinit var status: Status

        /** 行の読込方向 */
        lateinit var direction: Direction

        /** 読み込む行数 (行) */
        var lines: Int = -1

        /** 読込の開始位置 (byte)  */
        var offsetBytes: Long = 0L

        /** 読込の開始位置のモード */
        var offsetStart: OffsetStart = OffsetStart.head

        /**
         * オフセット位置からスキップする行数
         * 正値が与えられた場合、末尾方向にスキップします
         * 負値が与えられた場合、先頭方向にスキップします
         * 0 の場合はスキップを行いません
         */
        var skipLines: Int = 0

        /** ファイルが大きくなるのに合わせて追加されたデータを出力するかどうか */
        var follow: Boolean = false

        /** 検索文字列 */
        var search: Array<String> = arrayOf()

        fun build(fs: Fs) =
            Param().also { result ->
                if(::procedure.isInitialized) {
                    result.procedure = procedure
                }
                if(::path.isInitialized) {
                    result.path = fs.toAbsolutePath(path)
                }
                if(::status.isInitialized) {
                    result.status = status
                }
                if(::direction.isInitialized) {
                    result.direction = direction
                }
                result.lines = lines
                result.offsetBytes = offsetBytes
                result.offsetStart = offsetStart
                result.skipLines = skipLines
                result.isFollow = follow
                result.search = search
            }
    }
}
