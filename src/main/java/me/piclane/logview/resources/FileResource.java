package me.piclane.logview.resources;

import me.piclane.logview.fs.LogRoot;
import me.piclane.logview.procedure.text.CharsetDetector;

import javax.ws.rs.GET;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * ファイル関連の Api
 *
 * @author yohei_hina
 */
@javax.ws.rs.Path("/file")
public class FileResource {
    @GET
    @javax.ws.rs.Path("/download")
    public Response download(@QueryParam("path") String path) throws IOException {
        Path _path = LogRoot.of(path);
        String fn = _path.getFileName().toString();
        String fnl = fn.toLowerCase();
        Charset cs = CharsetDetector.detect(_path);
        long fileLen = Files.size(_path);

        String contentType;
        if(fnl.endsWith(".tsv")) {
            contentType = "text/tab-separated-values; charset=\"" + cs.name() + "\"";
        } else if(fnl.endsWith(".csv")) {
            contentType = "text/comma-separated-values; charset=\"" + cs.name() + "\"";
        } else {
            contentType = "text/plain; charset=\"" + cs.name() + "\"";
        }

        StreamingOutput stream = out -> {
            try(InputStream is = Files.newInputStream(_path);
                OutputStream os = out) {
                byte[] buf = new byte[3000];
                int readLen;
                long writtenLen = 0;
                while(fileLen > writtenLen && (readLen = is.read(buf)) != -1) {
                    if(fileLen < (writtenLen + readLen)) { // 予定した長さ以上の送信を抑止する
                        readLen = (int)(fileLen - writtenLen);
                    }
                    os.write(buf, 0, readLen);
                    writtenLen += readLen;
                }
            }
        };

        return Response.ok(stream)
                .header("Content-Type", contentType)
                .header("Content-Length", fileLen)
                .header("Content-Disposition", "attachment; filename=\"" + fn + "\"")
                .build();
    }
}
