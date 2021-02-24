package com.xxuz.piclane.logview.util

import com.fasterxml.jackson.databind.ObjectMapper
import java.io.Reader
import java.io.Writer

/**
 * JSON 直列化・非直列化ヘルパー
 *
 * @author yohei_hina
 */
object Json {
    /** JSON 非直列化用 [ObjectMapper] */
    private val mapper = ObjectMapper()

    /**
     * JSON を取得できる [Reader] からオブジェクトを生成します
     *
     * @param reader JSON を取得できる [Reader]
     * @param clazz 変換先オブジェクトのクラス
     * @return 変換されたオブジェクト
     */
    fun <T> deserialize(reader: Reader, clazz: Class<T>): T {
        return mapper.readValue(reader, clazz)
    }

    /**
     * JSON 文字列からオブジェクトを生成します
     *
     * @param json JSON
     * @param clazz 変換先オブジェクトのクラス
     * @return 変換されたオブジェクト
     */
    fun <T> deserialize(json: String, clazz: Class<T>): T {
        return mapper.readValue(json, clazz)
    }

    /**
     * オブジェクトを JSON に直列化します
     *
     * @param writer 直列化された JSONを書き込む [Writer]
     * @param src 変換元のオブジェクト
     */
    fun <T> serialize(writer: Writer, src: T) {
        mapper.writeValue(writer, src)
    }

    /**
     * オブジェクトを JSON に直列化します
     *
     * @param src 変換元のオブジェクト
     * @return 直列化された JSON
     */
    fun <T> serialize(src: T): String {
        return mapper.writeValueAsString(src)
    }
}
