package me.piclane.logview.procedure.text;

/**
 * 行の読込方向
 *
 * @author yohei_hina
 */
public enum Direction {
    /** 指定オフセットから順方向に 1 行ずつ読み込む */
    forward,

    /** 指定オフセットから逆方向に 1 行ずつ読み込む */
    backward;
}
