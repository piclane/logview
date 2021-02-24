package com.xxuz.piclane.logview.fs

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.ConstructorBinding
import java.nio.file.Path

/**
 * ファイルシステム設定
 *
 * @author yohei_hina
 */
@ConstructorBinding
@ConfigurationProperties("app.fs")
data class FsConfig(
        /** ルートディレクトリ (省略可能) */
        val root: Path?,

        /** ルートディレクトリにマウントするディレクトリへのマップ */
        val dirs: Map<String, Path> = emptyMap()
)
