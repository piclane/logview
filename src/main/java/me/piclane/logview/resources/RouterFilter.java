package me.piclane.logview.resources;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * /@/ と /$/ で始まる URL を index.html にフォワードするフィルタ
 */
@WebFilter(urlPatterns = "/*")
public class RouterFilter implements Filter {
    /** index.html にフォワードする ServletPath のパターン */
    private static final Pattern PATTERN_FORWARD_TO_INDEX = Pattern.compile("^/[@$]/.*$");

    /**
     * {@inheritDoc}
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req;
        HttpServletResponse resp;
        try {
            req = (HttpServletRequest)request;
            resp = (HttpServletResponse)response;
        } catch (ClassCastException e) {
            throw new ServletException("non-HTTP request or response");
        }

        String servletPath = req.getServletPath();
        if(PATTERN_FORWARD_TO_INDEX.matcher(servletPath).matches()) {
            RequestDispatcher dispatcher = req.getRequestDispatcher("/index.html");
            dispatcher.forward(req, resp);
        } else {
            chain.doFilter(req, resp);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void init(FilterConfig filterConfig) {
        // nop
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void destroy() {
        // nop
    }
}
