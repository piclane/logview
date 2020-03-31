package me.piclane.logview.util;

import me.piclane.logview.procedure.Signal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.websocket.Session;

/**
 * セッションと、セッションに紐付くタスクを管理します
 *
 * @author yohei_hina
 */
@WebListener
public class SessionTasks implements ServletContextListener {
    /** Logger */
    private static final Logger logger = LoggerFactory.getLogger(SessionTasks.class);

    /** 唯一のインスタンス */
    private static volatile SessionTasks instance = null;

    /**
     * 指定されたセッションに紐付くタスクを開始します
     *
     * @param session {@link Session}
     * @param runnable タスク
     */
    public static void start(Session session, Runnable runnable) {
        SessionTasks instance = SessionTasks.instance;
        if(instance == null) {
            throw new IllegalStateException("SessionTasks が開始されていません");
        }
        instance.startImpl(session, runnable);
    }

    /**
     * 指定されたセッションに紐付いた処理中のタスクを中断します
     *
     * @param session {@link Session}
     */
    public static void cancel(Session session) {
        SessionTasks instance = SessionTasks.instance;
        if(instance == null) {
            throw new IllegalStateException("SessionTasks が開始されていません");
        }
        instance.cancelImpl(session, SessionTasks::sendStoppedSignal);
    }

    /** スレッドキャッシュ */
    private final ExecutorService es = Executors.newCachedThreadPool();

    /** セッションID と {@link TaskRunner} のマップ */
    private final Map<String, TaskRunner> futures = new ConcurrentHashMap<>();

    /**
     * 指定されたセッションに紐付くタスクを開始します
     *
     * @param session {@link Session}
     * @param runnable タスク
     */
    private void startImpl(Session session, Runnable runnable) {
        cancelImpl(session, s -> {
            String id = session.getId();
            TaskRunner tr = new TaskRunner(id, runnable);
            tr.future = es.submit(tr);
            futures.put(id, tr);
        });
    }

    /**
     * 指定されたセッションに紐付いた処理中のタスクを中断します
     *
     * @param session {@link Session}
     * @param done セッションが完全に終了した後に呼び出されます
     */
    private void cancelImpl(Session session, Consumer<Session> done) {
        String id = session.getId();
        TaskRunner tr = futures.get(id);
        if(tr != null) {
            tr.future.cancel(true);
            es.submit(() -> {
                try {
                    tr.latch.await();
                } catch (InterruptedException e) {
                    // nop
                }
                done.accept(session);
            });
        } else {
            done.accept(session);
        }
    }

    /**
     * タスクの実行と、実行状態を保持します
     */
    private class TaskRunner implements Callable<Void> {
        /** タスクが完了したらラッチが外れる CountDownLatch */
        public final CountDownLatch latch = new CountDownLatch(1);

        /** セッションID */
        public final String sessionId;

        /** タスク */
        public final Runnable task;

        /** タスクの非同期計算の結果 */
        public Future<?> future;

        /**
         * コンストラクタ
         *
         * @param sessionId セッションID
         * @param task タスク
         */
        public TaskRunner(String sessionId, Runnable task) {
            this.sessionId = sessionId;
            this.task = task;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public Void call() {
            try {
                task.run();
            } finally {
                latch.countDown();
                futures.remove(sessionId);
            }
            return null;
        }
    }

    /**
     * 停止シグナルを送出します
     *
     * @param session {@link Session}
     */
    private static void sendStoppedSignal(Session session) {
        if(session.isOpen()) {
            try (Writer writer = session.getBasicRemote().getSendWriter()) {
                Json.serialize(new Object[]{Signal.STOPPED}, writer);
            } catch (IOException e) {
                logger.warn("Failed to send STOPPED signal.", e);
            }
        }
    }

    /**
     * @see javax.servlet.ServletContextListener#contextInitialized(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextInitialized(ServletContextEvent arg0) {
        SessionTasks.instance = this;
    }

    /**
     * @see javax.servlet.ServletContextListener#contextDestroyed(javax.servlet.ServletContextEvent)
     */
    @Override
    public void contextDestroyed(ServletContextEvent arg0) {
        SessionTasks instance = SessionTasks.instance;
        SessionTasks.instance = null;
        if(instance != null) {
            try {
                instance.es.shutdownNow();
                instance.es.awaitTermination(5L, TimeUnit.SECONDS);
            } catch (InterruptedException e) {
                // nop
            }
        }
    }
}
