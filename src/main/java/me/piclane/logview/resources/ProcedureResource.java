package me.piclane.logview.resources;

import me.piclane.logview.procedure.Param;
import me.piclane.logview.procedure.Procedure;
import me.piclane.logview.util.Json;
import me.piclane.logview.util.SessionTasks;

import javax.websocket.*;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;

/**
 * ファイル読み込みプロシージャ
 * 
 * @author yohei_hina
 */
@ServerEndpoint("/api/procedure")
public class ProcedureResource {
    /**
     * セッションが終了したときに呼び出されます
     * 
     * @param session {@link Session}
     */
    @OnClose
    public void onClose(Session session) {
        SessionTasks.cancel(session);
    }
    
    /**
     * セッションからメッセージを受信したときに呼び出されます
     * 
     * @param session {@link Session}
     * @param message メッセージ
     * @throws IOException 入出力例外が発生した場合
     */
    @OnMessage
    public void onMessage(Session session, String message) throws IOException {
        try {
            Param param = Json.deserialize(message, Param.class);
            Procedure proc = param.getProcedure();
            switch(param.getStatus()) {
                case start:
                    proc.start(session, param);
                    break;
                case stop:
                    SessionTasks.cancel(session);
                    break;
                default:
                    throw new IllegalArgumentException("Illegal status: " + param.getStatus());
            }
        } catch (IllegalArgumentException e) {
            session.close(new CloseReason(CloseCodes.CANNOT_ACCEPT, e.getMessage()));
        }
    }
    
}
