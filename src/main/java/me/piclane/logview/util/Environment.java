package me.piclane.logview.util;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 環境変数展開用ユーティリティー
 *
 * @author yohei_hina
 */
public class Environment {
    /** 展開対象のパターン */
    private static final Pattern PATTERN_PARAM = Pattern.compile("\\$\\{(?<name1>[^}]+)}|\\$(?<name2>[a-zA-Z0-9_]+)|(?:^(?<name3>@.*)$)");

    /** 展開マクロ */
    private static final Pattern PATTERN_MACRO = Pattern.compile("^(?<file>@?)(?<name>[^:]+)(:(?<sign>[-+])(?<alt>.*))?$");

    /**
     * 文字列内の環境変数を展開します
     *
     * @param string 文字列
     * @return 環境変数が展開された文字列
     */
    public static String expand(Object string) {
        if(string == null) {
            return null;
        }
        String src = string.toString();
        Matcher m = PATTERN_PARAM.matcher(src);
        StringBuilder buf = new StringBuilder();
        int pos = 0;
        while(m.find()) {
            int start = m.start(),
                end = m.end();
            String value, name = m.group("name1");
            if(name == null) {
                name = m.group("name2");
            }
            if(name == null) {
                name = m.group("name3");
            }
            Matcher m2 = PATTERN_MACRO.matcher(name);
            if(m2.matches()) {
                boolean isFile = Objects.equals("@", m2.group("file"));
                name = m2.group("name");
                value = isFile ? expand(readFile(name)) : System.getenv(name);
                boolean validVal = value != null && value.length() > 0;
                String sign = m2.group("sign");
                String altval = m2.group("alt");
                if(("-".equals(sign) && !validVal) || ("+".equals(sign) && validVal)) {
                    value = altval;
                }
            } else {
                value = System.getenv(name);
            }
            buf.append(src, pos, start);
            if(value != null) {
                buf.append(value);
            }
            pos = end;
        }
        buf.append(src, pos, src.length());
        return buf.toString();
    }

    /**
     * ファイルを読み込みます
     *
     * @param path 読み込むファイルのパス
     * @return ファイルの内容
     */
    private static String readFile(String path) {
        Path target = Paths.get(path);
        if(!Files.exists(target)) {
            return null;
        }
        try {
            return String.join("\n",
                Files.readAllLines(target, StandardCharsets.UTF_8));
        } catch(IOException e) {
            return null;
        }
    }
}
