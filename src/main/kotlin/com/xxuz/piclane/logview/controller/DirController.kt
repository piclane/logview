package com.xxuz.piclane.logview.controller

import com.xxuz.piclane.logview.fs.Fs
import com.xxuz.piclane.logview.fs.FsFile
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.*

/**
 * ディレクトリ関連の Api
 *
 * @author yohei_hina
 */
@RestController
@CrossOrigin
@RequestMapping(
        value = ["/api/dir"],
        produces = [MediaType.APPLICATION_JSON_VALUE])
class DirController {
    @Autowired
    private lateinit var fs: Fs

    @PostMapping("/list")
    fun list(@RequestParam("path") path: String, @RequestParam("query") query: String?): Collection<FsFile> =
            fs.toAbsolutePath(path).let {
                fs.list(it)
            }
}
