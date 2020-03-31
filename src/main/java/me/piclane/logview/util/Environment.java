package me.piclane.logview.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 環境変数展開用ユーティリティー
 *
 * @author yohei_hina
 */
public class Environment {
    /** 展開対象のパターン */
    private static final Pattern PATTERN_PARAM = Pattern.compile("\\$\\{(?<name1>[^}]+)}|\\$(?<name2>[a-zA-Z0-9_]+)");

    /** 展開マクロ */
    private static final Pattern PATTERN_MACRO = Pattern.compile("^([a-zA-Z0-9_]+):([-+])(.*)$");

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
            if(name != null) {
                Matcher m2 = PATTERN_MACRO.matcher(name);
                if(m2.matches()) {
                    name = m2.group(1);
                    value = System.getenv(name);
                    boolean validVal = value != null && value.length() > 0;
                    String sign = m2.group(2);
                    String altval = m2.group(3);
                    if(("-".equals(sign) && !validVal) || ("+".equals(sign) && validVal)) {
                        value = altval;
                    }
                } else {
                    value = System.getenv(name);
                }
            } else {
                name = m.group("name2");
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

    private static boolean isValidString(String s) {
        return s != null && s.length() > 0;
    }
}
