package me.piclane.logview.resources;

import me.piclane.logview.fs.LogFile;
import me.piclane.logview.fs.LogRoot;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.FileFilter;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;

/**
 * ディレクトリ関連の Api
 *
 * @author yohei_hina
 */
@javax.ws.rs.Path("/dir")
public class DirResource {
    /** ログファイルのための {@link FileFilter} */
    private static final DirectoryStream.Filter<Path> LOG_FILTER = new DirectoryStream.Filter<Path>() {
        @Override
        public boolean accept(Path entry) throws IOException {
            if(Files.isDirectory(entry)) {
                return true;
            }

            String name = entry.getFileName().toString().toLowerCase();
            if(name.endsWith(".gz") || name.endsWith(".bz2") || name.endsWith(".zip")) {
                return false;
            }
            if(Files.isHidden(entry)) {
                return false;
            }
            return true;
        }
    };

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @javax.ws.rs.Path("/list")
    public List<LogFile> list(@FormParam("path") String path, @FormParam("query") String query) throws IOException {
        Path _path = LogRoot.of(path);
        if(!Files.isDirectory(_path)) {
            throw new BadRequestException("path にはディレクトリを指定して下さい");
        }

        // ログファイル一覧取得
        List<LogFile> files = new LinkedList<>();
        if(_path.getNameCount() == 0) {
            for(LogRoot r: LogRoot.DIRS) {
                LogFile f = LogFile.from(r.dir);
                f.setName(r.name);
                files.add(f);
            }
        } else {
            for(Path p: Files.newDirectoryStream(_path, LOG_FILTER)) {
                files.add(LogFile.from(p));
            }
        }

        return files;
    }
}
