package com.xxuz.piclane.logview.controller

import com.xxuz.piclane.logview.util.Environment
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import javax.naming.Context
import javax.naming.InitialContext
import javax.naming.NamingException

/**
 * ヘッダー関連の Api
 *
 * @author yohei_hina
 */
@RestController
@CrossOrigin
@RequestMapping("/api/header")
class HeaderController {
    @Value("\${app.header:}")
    private lateinit var header: String

    @GetMapping(produces = [MediaType.TEXT_HTML_VALUE])
    fun header(): String {
        return header
    }
}
