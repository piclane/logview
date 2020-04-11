package me.piclane.logview.fs;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.GroupPrincipal;
import java.nio.file.attribute.PosixFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.UserPrincipal;
import java.util.Set;

/**
 * ログファイル
 *
 * @author yohei_hina
 */
public class LogFile {
    /**
     * {@link Path} から {@link LogFile} を生成します
     *
     * @param path パス
     * @return 生成された {@link LogFile}
     */
    public static LogFile from(Path path) {
        LogFile r = new LogFile();
        r.setName(path.getFileName().toString());
        try {
            r.setSize(Files.size(path));
        } catch(IOException e) {
            r.setSize(-1L);
        }
        try {
            r.setLastModified(Files.getLastModifiedTime(path).toMillis());
        } catch(IOException e) {
            r.setLastModified(-1L);
        }
        r.setType(Files.isDirectory(path) ? LogFile.FileType.dir : LogFile.FileType.file);
        r.setReadable(Files.isReadable(path));

        try {
            PosixFileAttributes attr = Files.readAttributes(path, PosixFileAttributes.class);
            UserPrincipal user = attr.owner();
            r.setOwner(user.getName());
            GroupPrincipal group = attr.group();
            r.setGroup(group.getName());
            Set<PosixFilePermission> perms = attr.permissions();
            int permsInt = 0;
            if(perms.contains(PosixFilePermission.OWNER_READ    )) permsInt |= 0x100;
            if(perms.contains(PosixFilePermission.OWNER_WRITE   )) permsInt |= 0x80;
            if(perms.contains(PosixFilePermission.OWNER_EXECUTE )) permsInt |= 0x40;
            if(perms.contains(PosixFilePermission.GROUP_READ    )) permsInt |= 0x20;
            if(perms.contains(PosixFilePermission.GROUP_WRITE   )) permsInt |= 0x10;
            if(perms.contains(PosixFilePermission.GROUP_EXECUTE )) permsInt |= 0x8;
            if(perms.contains(PosixFilePermission.OTHERS_READ   )) permsInt |= 0x4;
            if(perms.contains(PosixFilePermission.OTHERS_WRITE  )) permsInt |= 0x2;
            if(perms.contains(PosixFilePermission.OTHERS_EXECUTE)) permsInt |= 0x1;
            r.setPermissions(permsInt);
        } catch(IOException e) {
            // ignore
        }

        return r;
    }

    /** ファイル名 */
    private String name;

    /** ファイルサイズ */
    private long size;

    /** 最終更新日時 */
    private long lastModified;

    /** ファイルの所有者 */
    private String owner;

    /** ファイルのグループ */
    private String group;

    /** パーミッション */
    private int perms;

    /** ファイルタイプ */
    private FileType type;

    /** 読み取り可能かどうか */
    private boolean readable;

    /**
     * name を取得します
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * name を設定します
     *
     * @param name name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * size を取得します
     *
     * @return size
     */
    public long getSize() {
        return size;
    }

    /**
     * size を設定します
     *
     * @param size size
     */
    public void setSize(long size) {
        this.size = size;
    }

    /**
     * lastModified を取得します
     *
     * @return lastModified
     */
    public long getLastModified() {
        return lastModified;
    }

    /**
     * lastModified を設定します
     *
     * @param lastModified lastModified
     */
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }

    /**
     * owner を取得します
     *
     * @return owner
     */
    public String getOwner() {
        return owner;
    }

    /**
     * owner を設定します
     *
     * @param owner owner
     */
    public void setOwner(String owner) {
        this.owner = owner;
    }

    /**
     * group を取得します
     *
     * @return group
     */
    public String getGroup() {
        return group;
    }

    /**
     * group を設定します
     *
     * @param group group
     */
    public void setGroup(String group) {
        this.group = group;
    }

    /**
     * perms を取得します
     *
     * @return perms
     */
    public int getPermissions() {
        return perms;
    }

    /**
     * perms を設定します
     *
     * @param perms perms
     */
    public void setPermissions(int perms) {
        this.perms = perms;
    }

    /**
     * type を取得します
     *
     * @return type
     */
    public FileType getType() {
        return type;
    }

    /**
     * type を設定します
     *
     * @param type type
     */
    public void setType(FileType type) {
        this.type = type;
    }

    /**
     * readable を取得します
     *
     * @return readable
     */
    public boolean isReadable() {
        return readable;
    }

    /**
     * readable を設定します
     *
     * @param readable readable
     */
    public void setReadable(boolean readable) {
        this.readable = readable;
    }

    /** ファイル種別 */
    public enum FileType {
        /** ファイル */
        file,
        /** ディレクトリ */
        dir;
    }
}
