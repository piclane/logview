package me.piclane.logview.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

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
        instance.cancelImpl(session);
    }

    /** スレッドキャッシュ */
    private final ExecutorService es = Executors.newCachedThreadPool();

    /** セッションID と {@link Future} のマップ */
    private final Map<String, Future<?>> futures = new ConcurrentHashMap<>();

    /**
     * 指定されたセッションに紐付くタスクを開始します
     *
     * @param session {@link Session}
     * @param runnable タスク
     */
    private void startImpl(Session session, Runnable runnable) {
        cancelImpl(session);
        futures.computeIfAbsent(
                session.getId(),
                k -> es.submit(() -> {
                        runnable.run();
                        futures.remove(k);
                        return null;
                    }));
    }

    /**
     * 指定されたセッションに紐付いた処理中のタスクを中断します
     *
     * @param session {@link Session}
     */
    private void cancelImpl(Session session) {
        String id = session.getId();
        Future<?> future = futures.remove(id);
        if(future != null) {
            future.cancel(true);
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