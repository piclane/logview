package me.piclane.logview.resources;

import me.piclane.logview.util.Environment;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * ヘッダー関連の Api
 *
 * @author yohei_hina
 */
@javax.ws.rs.Path("/header")
public class HeaderResource {
    private static final String HEADER_HTML;

    static {
        String headerHtml = "";
        try {
            Context initContext = new InitialContext();
            Context envContext = (Context)initContext.lookup("java:/comp/env");
            Object _header = envContext.lookup("app/logview/header");
            if(_header != null) {
                headerHtml = Environment.expand(_header);
            }
        } catch (NamingException e) {
            // nop
        }
        HEADER_HTML = headerHtml;
    }

    @GET
    @Produces(MediaType.TEXT_HTML)
    public String header() {
        return HEADER_HTML;
    }
}
