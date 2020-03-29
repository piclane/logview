package me.piclane.logview.fs;

import javax.naming.*;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ログディレクトリルートを表現する
 *
 * @author yohei_hina
 */
public class LogRoot {
    /** 全てのログディレクトリルート */
    public static final List<LogRoot> DIRS;

    static {
        List<LogRoot> dirs = new LinkedList<>();
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context)initContext.lookup("java:/comp/env");
            NamingEnumeration<NameClassPair> ne = envContext.list("app/logview/dirs");
            while(ne.hasMoreElements()) {
                NameClassPair pair = ne.nextElement();
                String name = pair.getName();
                Object _dir = envContext.lookup("app/logview/dirs/" + name);
                Path dir = Paths.get(_dir.toString());
                if(!Files.isDirectory(dir)) {
                    continue;
                }
                dirs.add(new LogRoot(name, dir));
            }
        } catch (NamingException e) {
            throw new RuntimeException(e);
        }
        DIRS = Collections.unmodifiableList(dirs);
    }

    /** SERVLET_REQUEST_PARSER で使用するリクエスト文字列のパターン */
    private static final Pattern REQUEST_PATTERN = Pattern.compile("^/([^/]+)(/(.+))?$");

    public static Path of(String path) throws ClientErrorException {
        if("/".equals(path)) {
            return Paths.get("/");
        }

        Matcher m = REQUEST_PATTERN.matcher(path);
        if(m.find()) {
            Path rootDir = findDir(m.group(1));
            if(rootDir == null) {
                throw new BadRequestException("指定されたルートディレクトリは存在しません");
            }
            String relativePath = m.group(3);
            Path target = relativePath == null ? rootDir : rootDir.resolve(relativePath).normalize();
            if(!target.startsWith(rootDir)) {
                throw new BadRequestException("ルートディレクトリの外はアクセスできません");
            }
            if(!Files.exists(target)) {
                throw new BadRequestException("指定されたリソースは存在しません");
            }
            return target;
        } else {
            throw new BadRequestException("パスの形式が正しくありません");
        }
    }

    /**
     * ディレクトリの通称からディレクトリへのパスを取得します
     *
     * @param name ディレクトリの通称
     * @return ディレクトリへのパス、もしくは <code>null</code>
     */
    public static Path findDir(String name) {
        for(LogRoot dir: DIRS) {
            if(dir.name.equals(name)) {
                return dir.dir;
            }
        }
        return null;
    }

    /** ディレクトリの通称 */
    public final String name;

    /** ディレクトリへのパス */
    public final Path dir;

    /**
     * コンストラクタ
     *
     * @param name ディレクトリの通称
     * @param dir ディレクトリへのパス
     */
    private LogRoot(String name, Path dir) {
        super();
        this.name = name;
        this.dir = dir;
    }
}
