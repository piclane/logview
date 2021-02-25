package com.xxuz.piclane.logview.controller

import java.util.regex.Pattern
import javax.servlet.*
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

/**
 * /@/ と /$/ で始まる URL を index.html にフォワードするフィルタ
 */
class RouterFilter : Filter {
    /**
     * {@inheritDoc}
     */
    override fun doFilter(request: ServletRequest, response: ServletResponse, chain: FilterChain) {
        val req: HttpServletRequest
        val resp: HttpServletResponse
        try {
            req = request as HttpServletRequest
            resp = response as HttpServletResponse
        } catch (e: ClassCastException) {
            throw ServletException("non-HTTP request or response")
        }
        val servletPath = req.servletPath
        if (PATTERN_FORWARD_TO_INDEX.matcher(servletPath).matches()) {
            val dispatcher = req.getRequestDispatcher("/index.html")
            dispatcher.forward(req, resp)
        } else {
            chain.doFilter(req, resp)
        }
    }

    /**
     * {@inheritDoc}
     */
    override fun init(filterConfig: FilterConfig) {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    override fun destroy() {
        // nop
    }

    companion object {
        /** index.html にフォワードする ServletPath のパターン  */
        private val PATTERN_FORWARD_TO_INDEX = Pattern.compile("^/[@$]/.*$")
    }
}
