package com.xxuz.piclane.logview.procedure

import org.springframework.web.socket.WebSocketSession

/**
 * プロシージャ
 *
 * @author yohei_hina
 */
enum class Procedure {
    /** 通常の読込  */
    read,

    /** 検索  */
    search,

    /** かしこい検索  */
    searchSmart;

    /**
     * タスクを生成します
     *
     * @param session [WebSocketSession]
     * @param param パラメーター
     */
    fun buildTask(session: WebSocketSession, param: Param): () -> Unit =
        when (this) {
            read -> Reader(session, param)::run
            search -> PlainSearch(session, param)::run
            searchSmart -> SmartSearch(session, param)::run
        }
}
