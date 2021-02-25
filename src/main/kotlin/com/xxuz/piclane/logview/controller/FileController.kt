package com.xxuz.piclane.logview.controller

import com.xxuz.piclane.logview.fs.Fs
import com.xxuz.piclane.logview.procedure.text.CharsetDetector
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.nio.file.Files

/**
 * ファイル関連の Api
 *
 * @author yohei_hina
 */
@RestController
@CrossOrigin
@RequestMapping(
        value = ["/api/file"])
class FileController {
    @Autowired
    private lateinit var fs: Fs

    @GetMapping("/download")
    fun download(@RequestParam("path") path: String): ResponseEntity<StreamingResponseBody> {
        val physicalPath = fs.toAbsolutePath(path)
        val filename = physicalPath.fileName.toString()
        val filenameLower = filename.toLowerCase()
        val cs = CharsetDetector.detect(physicalPath)
        val fileLen = Files.size(physicalPath)
        val contentType = when {
            filenameLower.endsWith(".tsv") -> "text/tab-separated-values; charset=\"${cs.name()}\""
            filenameLower.endsWith(".csv") -> "text/comma-separated-values; charset=\"${cs.name()}\""
            else -> "text/plain; charset=\"${cs.name()}\""
        }

        return ResponseEntity.ok()
                .contentType(MediaType(contentType))
                .contentLength(fileLen)
                .header("Content-Disposition", "attachment; filename=\"$filename\"")
                .body(StreamingResponseBody { os ->
                    Files.newInputStream(physicalPath).use { input ->
                        val buf = ByteArray(3000)
                        var writtenLen = 0L
                        while (fileLen > writtenLen) {
                            var readLen = input.read(buf)
                            if(readLen == -1) {
                                break
                            }
                            if (fileLen < writtenLen + readLen) { // 予定した長さ以上の送信を抑止する
                                readLen = (fileLen - writtenLen).toInt()
                            }
                            os.write(buf, 0, readLen)
                            writtenLen += readLen.toLong()
                        }
                    }
                })
    }
}
