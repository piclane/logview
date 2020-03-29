package me.piclane.logview.procedure;

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import me.piclane.logview.fs.LogRoot;
import me.piclane.logview.procedure.text.Direction;
import me.piclane.logview.procedure.text.OffsetStart;

import java.io.IOException;
import java.nio.file.Path;

/**
 * パラメーター
 * 
 * @author yohei_hina
 */
public class Param {
    /** プロシージャ */
    private Procedure procedure = null;

    /** パス */
    @JsonAdapter(PathAdapter.class)
    private Path path = null;

    /** ステータス */
    private Status status = null;
    
    /** 行の読込方向 */
    private Direction direction = null;
    
    /** 読み込む行数 (行)  */
    private int lines = -1;

    /** 読込の開始位置 (byte) */
    private long offsetBytes = 0;

    /** 読込の開始位置のモード */
    private OffsetStart offsetStart = null;

    /**
     * オフセット位置からスキップする行数
     * 正値が与えられた場合、末尾方向にスキップします
     * 負値が与えられた場合、先頭方向にスキップします
     * 0 の場合はスキップを行いません
     */
    private int skipLines = 0;

    /** ファイルが大きくなるのに合わせて追加されたデータを出力するかどうか */
    @SerializedName("follow")
    private boolean isFollow = false;

    /** 検索文字列 */
    private String[] search;

    /**
     * procedure を取得します
     *
     * @return procedure の値
     */
    public Procedure getProcedure() {
        return procedure;
    }

    /**
     * procedure を設定します
     *
     * @param procedure procedure の値
     */
    public void setProcedure(Procedure procedure) {
        this.procedure = procedure;
    }

    /**
     * path を取得します
     *
     * @return path の値
     */
    public Path getPath() {
        return path;
    }

    /**
     * path を設定します
     *
     * @param path path の値
     */
    public void setPath(Path path) {
        this.path = path;
    }

    /**
     * status を取得します
     *
     * @return status
     */
    public Status getStatus() {
        return status;
    }

    /**
     * status を設定します
     * 
     * @param status status
     */
    public void setStatus(Status status) {
        this.status = status;
    }

    /**
     * direction を取得します
     *
     * @return direction
     */
    public Direction getDirection() {
        return direction;
    }

    /**
     * direction を設定します
     * 
     * @param direction direction
     */
    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    /**
     * lines を取得します
     *
     * @return lines
     */
    public int getLines() {
        if(this.lines < 0) {
            return Integer.MAX_VALUE;
        }
        return lines;
    }

    /**
     * lines を設定します
     * 
     * @param lines lines
     */
    public void setLines(int lines) {
        this.lines = lines;
    }

    /**
     * offset を取得します
     *
     * @return offset
     */
    public long getOffsetBytes() {
        return offsetBytes;
    }

    /**
     * offset を設定します
     * 
     * @param offset offset
     */
    public void setOffsetBytes(long offset) {
        this.offsetBytes = offset;
    }

    /**
     * offsetMode を取得します
     *
     * @return offsetMode の値
     */
    public OffsetStart getOffsetStart() {
        return offsetStart;
    }

    /**
     * offsetMode を設定します
     *
     * @param offsetStart offsetMode の値
     */
    public void setOffsetStart(OffsetStart offsetStart) {
        this.offsetStart = offsetStart;
    }

    /**
     * skipLines を取得します
     *
     * @return skipLines の値
     */
    public int getSkipLines() {
        return skipLines;
    }

    /**
     * skipLines を設定します
     *
     * @param skipLines skipLines の値
     */
    public void setSkipLines(int skipLines) {
        this.skipLines = skipLines;
    }

    /**
     * isFollow を取得します
     *
     * @return follow
     */
    public boolean isFollow() {
        return isFollow && direction == Direction.forward;
    }

    /**
     * isFollow を設定します
     * 
     * @param follow follow
     */
    public void setFollow(boolean follow) {
        this.isFollow = follow;
    }

    /**
     * queries を取得します
     *
     * @return queries の値
     */
    public String[] getSearch() {
        return search;
    }

    /**
     * queries を設定します
     *
     * @param search queries の値
     */
    public void setSearch(String[] search) {
        this.search = search;
    }

    /**
     * Path の為の TypeAdapter
     */
    private static class PathAdapter extends TypeAdapter<Path> {
        @Override
        public void write(JsonWriter out, Path value) throws IOException {
            // nop
        }

        @Override
        public Path read(JsonReader in) throws IOException {
            return LogRoot.of(in.nextString());
        }
    }
}
