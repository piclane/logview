import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "2.4.3"
    id("io.spring.dependency-management") version "1.0.11.RELEASE"
    kotlin("jvm") version "1.4.30"
    kotlin("plugin.spring") version "1.4.30"
}

group = "com.xxuz.piclane"
version = "2.1-SNAPSHOT"
java.sourceCompatibility = JavaVersion.VERSION_11

repositories {
    mavenCentral()
    jcenter()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    implementation("com.github.albfernandez:juniversalchardet:2.2.0")
    implementation("jchardet:jchardet:1.1.0")

    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.junit.jupiter:junit-jupiter-migrationsupport:5.7.1")
    testImplementation("javax.websocket:javax.websocket-api:1.1")
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    testImplementation("com.google.jimfs:jimfs:1.1")
    testImplementation("org.mockito:mockito-core:2.+")
    testImplementation("org.mockito:mockito-junit-jupiter:2.+")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "11"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.register("buildNpm") {
    group = "build"
    description = "npm run build"
    doLast {
        project.exec {
            commandLine("npm", "install")
        }

        project.exec {
            commandLine("npm", "run", "build")
        }

        ant.withGroovyBuilder {
            "move"("todir" to "${buildDir}/resources/main/static/", "overwrite" to false) {
                "fileset"("dir" to "${projectDir}/dist/")
            }
        }
    }
}

tasks.bootJar {
    dependsOn(tasks.getByName("buildNpm"))
}
