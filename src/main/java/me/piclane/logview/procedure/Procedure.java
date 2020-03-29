package me.piclane.logview.procedure;

import javax.websocket.Session;

import me.piclane.logview.util.SessionTasks;

/**
 * プロシージャ
 * 
 * @author yohei_hina
 */
public enum Procedure {
    /** 通常の読込 */
    read,

    /** 検索 */
    search,

    /** かしこい検索 */
    searchSmart;

    /**
     * ログの送信を開始します
     * 
     * @param session {@link Session}
     * @param param パラメーター
     */
    public void start(Session session, Param param) {
        switch(this) {
            case read:
                SessionTasks.start(session, new Reader(session, param));
                break;
            case search:
                SessionTasks.start(session, new PlainSearch(session, param));
                break;
            case searchSmart:
                SessionTasks.start(session, new SmartSearch(session, param));
                break;
            default:
                throw new InternalError();
        }
    }
}
