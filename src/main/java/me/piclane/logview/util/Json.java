package me.piclane.logview.util;

import com.google.gson.*;

import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * JSON 直列化・非直列化ヘルパー
 *
 * @author yohei_hina
 */
public class Json {
    /**
     * JSON を取得できる {@link Reader} からオブジェクトを生成します
     *
     * @param reader JSON を取得できる {@link Reader}
     * @param clazz 変換先オブジェクトのクラス
     * @return 変換されたオブジェクト
     */
    public static <T> T deserialize(Reader reader, Class<T> clazz) {
        return gson().fromJson(reader, clazz);
    }

    /**
     * JSON 文字列からオブジェクトを生成します
     *
     * @param json JSON
     * @param clazz 変換先オブジェクトのクラス
     * @return 変換されたオブジェクト
     */
    public static <T> T deserialize(String json, Class<T> clazz) {
        return gson().fromJson(json, clazz);
    }

    /**
     * オブジェクトを JSON に直列化します
     *
     * @param src 変換元のオブジェクト
     * @param writer 直列化された JSONを書き込む {@link Writer}
     */
    public static <T> void serialize(T src, Writer writer) {
        gson().toJson(src, writer);
    }

    /**
     * オブジェクトを JSON に直列化します
     *
     * @param src 変換元のオブジェクト
     * @return 直列化された JSON
     */
    public static <T> String serialize(T src) {
        return gson().toJson(src);
    }

    /**
     * オブジェクトを JSON に直列化し、UTF-8 で表現されるバイト配列に変換します
     *
     * @param src 変換元のオブジェクト
     * @return 直列化された JSON
     */
    public static <T> byte[] serializeToByte(T src) {
        return serialize(src).getBytes(StandardCharsets.UTF_8);
    }

    /**
     * {@link Gson} を取得します
     *
     * @return {@link Gson}
     */
    private static Gson gson() {
        return new GsonBuilder()
                .registerTypeAdapter(URL.class, UrlTypeAdapter.instance)
                .registerTypeAdapter(URL[].class, UrlArrayTypeAdapter.instance)
                .registerTypeAdapter(Duration.class, DurationTypeAdapter.instance)
                .registerTypeAdapter(LocalDateTime.class, LocalDateTimeTypeAdapter.instance)
                .registerTypeAdapter(UUID.class, UUIDTypeAdapter.instance)
                .create();
    }

    private static class UrlTypeAdapter implements JsonSerializer<URL>, JsonDeserializer<URL> {
        public static final UrlTypeAdapter instance = new UrlTypeAdapter();

        /**
         * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
         */
        @Override
        public URL deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                return new URL(json.getAsString());
            } catch (MalformedURLException e) {
                throw new JsonParseException(e);
            }
        }

        /**
         * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
         */
        @Override
        public JsonElement serialize(URL src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toExternalForm());
        }
    }

    private static class UrlArrayTypeAdapter implements JsonSerializer<URL[]>, JsonDeserializer<URL[]> {
        public static final UrlArrayTypeAdapter instance = new UrlArrayTypeAdapter();

        /**
         * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
         */
        @Override
        public URL[] deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            try {
                JsonArray array = json.getAsJsonArray();
                URL[] urls = new URL[array.size()];
                for(int i=0; i<urls.length; i++) {
                    urls[i] = new URL(array.get(i).getAsString());
                }
                return urls;
            } catch (MalformedURLException e) {
                throw new JsonParseException(e);
            }
        }

        /**
         * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
         */
        @Override
        public JsonElement serialize(URL[] src, Type typeOfSrc, JsonSerializationContext context) {
            JsonArray array = new JsonArray();
            for(URL url: src) {
                array.add(new JsonPrimitive(url.toExternalForm()));
            }
            return array;
        }
    }

    private static class DurationTypeAdapter implements JsonSerializer<Duration>, JsonDeserializer<Duration> {
        public static final DurationTypeAdapter instance = new DurationTypeAdapter();

        /**
         * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
         */
        @Override
        public Duration deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            return Duration.parse(json.getAsString());
        }

        /**
         * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
         */
        @Override
        public JsonElement serialize(Duration src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }

    private static class LocalDateTimeTypeAdapter implements JsonSerializer<LocalDateTime>, JsonDeserializer<LocalDateTime> {
        public static final LocalDateTimeTypeAdapter instance = new LocalDateTimeTypeAdapter();

        /**
         * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
         */
        @Override
        public LocalDateTime deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String value = json.getAsString();
            try {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
            } catch (DateTimeParseException e) {
                return LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        }

        /**
         * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
         */
        @Override
        public JsonElement serialize(LocalDateTime src, Type typeOfSrc, JsonSerializationContext context) {
            ZonedDateTime dateTime = src.atZone(ZoneId.systemDefault());

            return new JsonPrimitive(dateTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
        }
    }

    private static class UUIDTypeAdapter implements JsonSerializer<UUID>, JsonDeserializer<UUID> {
        public static final UUIDTypeAdapter instance = new UUIDTypeAdapter();

        /**
         * @see com.google.gson.JsonDeserializer#deserialize(com.google.gson.JsonElement, java.lang.reflect.Type, com.google.gson.JsonDeserializationContext)
         */
        @Override
        public UUID deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            String uuid = json.getAsString();
            return UUID.fromString(uuid);
        }

        /**
         * @see com.google.gson.JsonSerializer#serialize(java.lang.Object, java.lang.reflect.Type, com.google.gson.JsonSerializationContext)
         */
        @Override
        public JsonElement serialize(UUID src, Type typeOfSrc, JsonSerializationContext context) {
            return new JsonPrimitive(src.toString());
        }
    }
}
