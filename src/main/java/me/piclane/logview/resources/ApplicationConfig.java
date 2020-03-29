package me.piclane.logview.resources;

import org.glassfish.jersey.server.ResourceConfig;

import javax.ws.rs.ApplicationPath;

/**
 * アプリケーション設定
 */
@ApplicationPath("/api")
public class ApplicationConfig extends ResourceConfig {
    /**
     * コンストラクタ
     */
    public ApplicationConfig() {
        // resource を含むパッケージ
        packages(ApplicationConfig.class.getPackage().toString());
    }
}
