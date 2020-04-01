package me.piclane.logview.fs;

import me.piclane.logview.util.Environment;

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
    /** ルートディレクトリ */
    public static final Path ROOT;

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
                Path dir = Paths.get(Environment.expand(_dir));
                if(!Files.isDirectory(dir)) {
                    continue;
                }
                dirs.add(new LogRoot(name, dir));
            }
        } catch (NamingException e) {
            // nop
        }
        DIRS = Collections.unmodifiableList(dirs);

        Path root = null;
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context)initContext.lookup("java:/comp/env");
            Object _root = envContext.lookup("app/logview/rootDir");
            if(_root != null) {
                root = Paths.get(Environment.expand(_root));
            }
        } catch (NamingException e) {
            // nop
        }
        ROOT = root;
    }

    /** パスのパターン */
    private static final Pattern PATH_PATTERN = Pattern.compile("^/([^/]+)(/(.+))?$");

    public static Path of(String path) throws ClientErrorException {
        if("/".equals(path)) {
            return Paths.get("/");
        }

        Matcher m = PATH_PATTERN.matcher(path);
        if(m.find()) {
            String rootDirName = m.group(1);
            Path rootDir = findDir(rootDirName);
            if(rootDir == null && ROOT != null) {
                Path _rootDir = ROOT.resolve(rootDirName);
                if(Files.exists(_rootDir)) {
                    rootDir = _rootDir;
                }
            }
            if(rootDir == null) {
                throw new BadRequestException("The specified directory does not exist.");
            }
            String relativePath = m.group(3);
            Path target = relativePath == null ? rootDir : rootDir.resolve(relativePath).normalize();
            if(!target.startsWith(rootDir)) {
                throw new BadRequestException("The specified resource is forbidden.");
            }
            if(!Files.exists(target)) {
                throw new BadRequestException("The specified resource does not exist.");
            }
            return target;
        } else {
            throw new BadRequestException("Invalid path format.");
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
