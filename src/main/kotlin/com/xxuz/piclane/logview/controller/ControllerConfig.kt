package com.xxuz.piclane.logview.controller

import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.CorsRegistry

import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer


@Configuration
@EnableWebSocket
class ControllerConfig {
    @Bean
    fun routerFilter() =
            FilterRegistrationBean(RouterFilter()).also {
                it.addUrlPatterns("/*")
            }

    @Bean
    fun corsConfig(): WebMvcConfigurer =
            object : WebMvcConfigurer {
                override fun addCorsMappings(registry: CorsRegistry) {
                    registry.addMapping("/**").allowedOrigins("*")
                }
            }

    @Bean
    fun webSocketConfig(): WebSocketConfigurer =
            WebSocketConfigurer { registry ->
                registry
                        .addHandler(procedureController(), "/api/procedure")
                        .setAllowedOrigins("*")
            }

    @Bean
    fun procedureController() = ProcedureController()
}
