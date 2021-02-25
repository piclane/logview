package com.xxuz.piclane.logview.fs

import com.fasterxml.jackson.annotation.JsonProperty
import java.io.IOException
import java.nio.file.*
import java.nio.file.attribute.PosixFileAttributes
import java.nio.file.attribute.PosixFilePermission

/**
 * ログファイル
 *
 * @author yohei_hina
 */
data class FsFile(
        /** ファイル名  */
        val name: String,
        /** ファイルタイプ  */
        val type: FileType,
        /** ファイルサイズ  */
        val size: Long = 0L,
        /** 最終更新日時  */
        val lastModified: Long = 0L,
        /** ファイルの所有者  */
        val owner: String = "",
        /** ファイルのグループ  */
        val group: String = "",
        /** パーミッション  */
        val permissions: Int = 0,
        /** 読み取り可能かどうか  */
        @JsonProperty("readable")
        val isReadable: Boolean = false
) {
    /** ファイル種別  */
    enum class FileType {
        /** ファイル  */
        file,

        /** ディレクトリ  */
        dir
    }

    companion object {
        /**
         * [Path] から [FsFile] を生成します
         *
         * @param path パス
         * @return 生成された [FsFile]
         */
        fun from(path: Path, name: String? = null): FsFile {
            var owner = ""
            var group = ""
            var permissions = 0
            try {
                val attr = Files.readAttributes(path, PosixFileAttributes::class.java)
                owner = attr.owner().name
                group = attr.group().name
                permissions = attr.permissions().let { perms ->
                    var permsInt = 0
                    if (perms.contains(PosixFilePermission.OWNER_READ)) permsInt = permsInt or 0x100
                    if (perms.contains(PosixFilePermission.OWNER_WRITE)) permsInt = permsInt or 0x80
                    if (perms.contains(PosixFilePermission.OWNER_EXECUTE)) permsInt = permsInt or 0x40
                    if (perms.contains(PosixFilePermission.GROUP_READ)) permsInt = permsInt or 0x20
                    if (perms.contains(PosixFilePermission.GROUP_WRITE)) permsInt = permsInt or 0x10
                    if (perms.contains(PosixFilePermission.GROUP_EXECUTE)) permsInt = permsInt or 0x8
                    if (perms.contains(PosixFilePermission.OTHERS_READ)) permsInt = permsInt or 0x4
                    if (perms.contains(PosixFilePermission.OTHERS_WRITE)) permsInt = permsInt or 0x2
                    if (perms.contains(PosixFilePermission.OTHERS_EXECUTE)) permsInt = permsInt or 0x1
                    permsInt
                }
            } catch (e: IOException) {
                // ignore
            }

            return FsFile(
                    name = name ?: path.fileName.toString(),
                    size = try {
                        Files.size(path)
                    } catch (e: IOException) {
                        -1L
                    },
                    lastModified = try {
                        Files.getLastModifiedTime(path).toMillis()
                    } catch (e: IOException) {
                        -1L
                    },
                    type = if (Files.isDirectory(path)) FileType.dir else FileType.file,
                    isReadable = Files.isReadable(path),
                    owner = owner,
                    group = group,
                    permissions = permissions
            )
        }
    }
}
