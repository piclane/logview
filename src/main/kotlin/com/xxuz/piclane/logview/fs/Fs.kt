package com.xxuz.piclane.logview.fs

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.nio.file.DirectoryStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.*
import java.util.regex.Pattern

/**
 * ログ用のファイルシステムの様なもの
 *
 * @author yohei_hina
 */
@Service
class Fs {
    @Autowired
    private lateinit var config: FsConfig

    /**
     * 指定された論理パス文字列を物理パスに変換します
     *
     * @param path 論理パス文字列
     */
    fun toAbsolutePath(path: String): Path {
        if ("/" == path) {
            return Paths.get("/")
        }
        val m = PATH_PATTERN.matcher(path)
        return if (m.find()) {
            val rootDirName = m.group(1)
            val rootDir = config.dirs[rootDirName].let baseDir@ { baseDir ->
                if (baseDir == null) {
                    config.root?.resolve(rootDirName)?.also {
                        if (Files.exists(it)) {
                            return@baseDir it
                        }
                    }
                }
                return@baseDir baseDir
            } ?: throw IllegalArgumentException("The specified directory does not exist.")
            val relativePath = m.group(3)
            val target = if (relativePath == null) rootDir else rootDir.resolve(relativePath).normalize()
            if (!target.startsWith(rootDir)) {
                throw IllegalArgumentException("The specified resource is forbidden.")
            }
            if (!Files.exists(target)) {
                throw IllegalArgumentException("The specified resource does not exist.")
            }
            target
        } else {
            throw IllegalArgumentException("Invalid path format.")
        }
    }

    /**
     * 指定された物理パスがディレクトリを指す場合、ディレクトリの中のファイルをすべて取得します
     *
     * @param path リストアップしたいディレクトリの物理パス
     */
    fun list(path: Path): Collection<FsFile> {
        if (!Files.isDirectory(path)) {
            throw IllegalArgumentException("path にはディレクトリを指定して下さい")
        }

        // ログファイル一覧取得
        return if (path.nameCount == 0) {
            LinkedHashSet<FsFile>().also { files ->
                config.dirs.entries.forEach {
                    files.add(FsFile.from(it.value, it.key))
                }
                if (config.root != null) {
                    for (p in Files.newDirectoryStream(config.root!!, LOG_FILTER)) {
                        files.add(FsFile.from(p))
                    }
                }
            }
        } else {
            LinkedList<FsFile>().also { files ->
                for (p in Files.newDirectoryStream(path, LOG_FILTER)) {
                    files.add(FsFile.from(p))
                }
            }
        }
    }

    companion object {
        /** パスのパターン  */
        private val PATH_PATTERN = Pattern.compile("^/([^/]+)(/(.+))?$")

        /** ログファイルのための [DirectoryStream.Filter]  */
        private val LOG_FILTER = DirectoryStream.Filter<Path> { entry ->
            if (Files.isDirectory(entry)) {
                return@Filter true
            }
            val name = entry.fileName.toString().toLowerCase()
            if (name.endsWith(".gz") || name.endsWith(".bz2") || name.endsWith(".zip")) {
                return@Filter false
            }
            !Files.isHidden(entry)
        }
    }
}
