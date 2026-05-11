plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.2"
}

group = "com.muyan"
version = "1.0.4"

repositories {
    mavenCentral()
}

dependencies {
    // YAML 解析库
    implementation("org.yaml:snakeyaml:2.2")
    // JSON 处理（用于 YAML↔JSON 转换）
    implementation("com.google.code.gson:gson:2.10.1")

    // 测试
    testImplementation("junit:junit:4.13.2")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

intellij {
    // IntelliJ IDEA Community Edition 版本
    version.set("2023.3")
    type.set("IC") // IC = Community, IU = Ultimate

    // 依赖的内置插件
    plugins.set(listOf(
        "com.intellij.java",     // Java 支持（可选）
        "org.jetbrains.plugins.yaml", // YAML 语言支持
        "com.intellij.properties" // Properties 语言支持
    ))
}

tasks {
    // 设置 JVM 版本
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
        options.encoding = "UTF-8"
    }

    patchPluginXml {
        // 支持的 IDEA 版本范围
        sinceBuild.set("233")       // 2023.3+
        untilBuild.set("243.*")     // 到 2024.3.x
    }

    runIde {
        args = listOf(file("sandbox-playground").absolutePath)
    }

    signPlugin {
        certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
        privateKey.set(System.getenv("PRIVATE_KEY"))
        password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
    }

    publishPlugin {
        token.set(System.getenv("PUBLISH_TOKEN"))
    }
}
