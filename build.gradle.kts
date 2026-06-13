import java.util.*

plugins {
    id("fabric-loom") version "1.15.5"
    id("io.freefair.lombok") version "8.14"
}


val versionsDir = rootProject.file("versions")
val versions = versionsDir.list().map { name -> name.replace(".properties", "") }
println("all version => $versions")
// 获取当前构建的版本（从命令行参数或默认值）
val currentMcVersion =
    project.findProperty("minecraft_version")?.toString() ?: properties["minecraft_version"].toString()

println("load properties to => ${currentMcVersion}.properties")
val prop = Properties().apply {
    val file = rootProject.file("versions/${currentMcVersion}.properties")
    if (file.exists()) {
        load(file.inputStream())
    }
}



base {
    archivesName = "${properties["archives_base_name"] as String}-$currentMcVersion"
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    // 允许从 libs/ 目录加载 flat jar 文件
    flatDir {
        dirs("libs")
    }

    exclusiveContent {
        forRepository {
            maven {
                name = "Modrinth"
                url = uri("https://api.modrinth.com/maven")
            }
        }
        filter {
            includeGroup("maven.modrinth")
        }
    }

    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }

    maven { url = uri("https://maven.seedfinding.com/") }
    maven { url = uri("https://maven-snapshots.seedfinding.com/") }
    maven { url = uri("https://maven.duti.dev/releases") }
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://masa.dy.fi/maven") }
    maven { url = uri("https://maven.terraformersmc.com/releases/") }
    maven { url = uri("https://maven.fallenbreath.me/releases") }

    mavenCentral()


}

// Configuration that holds jars to include in the jar
val extraLibs: Configuration by configurations.creating

// ProGuard 配置
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.8.0")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:${currentMcVersion}")
    mappings("net.fabricmc:yarn:${prop.getProperty("yarn_mappings")}:v2")

    modImplementation("net.fabricmc:fabric-loader:${prop.getProperty("loader_version")}")

//    implementation("com.guardsquare:proguard-gradle:7.8.0")

    // 使用配置中定义的meteor版本
    modImplementation("meteordevelopment:meteor-client:${prop.getProperty("meteor_version")}")

    // XaeroPlus https://modrinth.com/mod/xaeroplus/version/2.27.2+fabric-1.21.1
    modImplementation("maven.modrinth:xaeroplus:${prop.getProperty("xaeroplus_version")}")
    modImplementation("maven.modrinth:xaeros-world-map:${prop.getProperty("xaeros_worldmap_version")}")
    modImplementation("maven.modrinth:xaeros-minimap:${prop.getProperty("xaeros_minimap_version")}")
    modCompileOnly("meteordevelopment:baritone:${prop.getProperty("baritone_version")}")

    // XaeroWorldMap https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21
    //               https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21.4
    // XaeroMinimap https://modrinth.com/mod/xaeros-minimap/version/25.2.10_Fabric_1.21
//    modImplementation("maven.modrinth:litematica:0.19.59")

    modImplementation("com.github.sakura-ryoko:malilib:${prop.getProperty("malilib_version")}")
    modImplementation("com.github.sakura-ryoko:litematica:${prop.getProperty("litematica_version")}")
    modImplementation("com.github.sakura-ryoko:minihud:${prop.getProperty("minihud_version")}")
    modImplementation(files("libs/jefff-mod-0.16.6-1.21.1.jar"))

//    modImplementation("com.github.sakura-ryoko:malilib:1.21-0.21.9")
//    modImplementation("com.github.sakura-ryoko:litematica:1.21-0.19.59")
//    modImplementation("com.github.sakura-ryoko:minihud:1.21-0.32.58")

    extraLibs("dev.duti.acheong:cubiomes:1.22.5") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:linux64") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:osx") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:windows64") { isTransitive = false }

    extraLibs("com.seedfinding:mc_biome:1.171.1") { isTransitive = false }
    extraLibs("com.seedfinding:mc_core:1.210.0") { isTransitive = false }
    extraLibs("com.seedfinding:mc_feature:1.171.10") { isTransitive = false }
    extraLibs("com.seedfinding:mc_math:1.171.0") { isTransitive = false }
    extraLibs("com.seedfinding:mc_noise:1.171.1") { isTransitive = false }
    extraLibs("com.seedfinding:mc_seed:1.171.2") { isTransitive = false }
    extraLibs("com.seedfinding:mc_terrain:1.171.1") { isTransitive = false }

    configurations.implementation.get().extendsFrom(extraLibs)

}

// 为每个版本创建构建任务
versions.forEach { versionName ->
    val taskName = "buildFor${versionName.replace(".", "")}"

    tasks.register(taskName, Exec::class) {
        group = "build"
        description = "Build jar for Minecraft $versionName"

        workingDir = projectDir

        // 根据操作系统选择命令
        if (System.getProperty("os.name").lowercase().contains("windows")) {
            commandLine(
                "cmd",
                "/c",
                "gradlew.bat",
                "clean",
                "build",
                "-x",
                "test",
                "--no-configuration-cache",
                "-Pminecraft_version=$versionName"
            )
        } else {
            commandLine(
                "./gradlew",
                "clean",
                "build",
                "-x",
                "test",
                "--no-configuration-cache",
                "-Pminecraft_version=$versionName"
            )
        }

        // 构建完成后复制jar文件到版本特定的目录
        doLast {
            val buildLibsDir = file("build/libs")
            val versionDir = file("build/versions/$versionName")
            versionDir.mkdirs()

            buildLibsDir.listFiles()?.forEach { jarFile ->
                if (jarFile.name.endsWith(".jar") && !jarFile.name.contains("sources")) {
                    val targetFile = File(versionDir, jarFile.name)
                    jarFile.copyTo(targetFile, overwrite = true)
                    println("Copied ${jarFile.name} to ${targetFile.absolutePath}")
                }
            }
        }
    }
}

// 创建构建所有版本的任务
tasks.register("buildAll") {
    group = "build"
    description = "Build jars for all Minecraft versions"

    dependsOn(versions.map { "buildFor${it.replace(".", "")}" })
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 在编译前生成版本特定类
tasks.register("prepareVersionSpecificSources") {
    doLast {
        val utilDir = file("src/main/java/com/github/mikumiku/addon/util")
        utilDir.mkdirs()

        val sourceDir = when (currentMcVersion) {
            "1.21.1" -> file("src/main/java/com/github/mikumiku/addon/v1211")
            "1.21.4" -> file("src/main/java/com/github/mikumiku/addon/v1214")
            "1.21.8" -> file("src/main/java/com/github/mikumiku/addon/v1218")
            "1.21.11" -> file("src/main/java/com/github/mikumiku/addon/v12111")
            else -> throw GradleException("Unsupported version: $currentMcVersion")
        }

        if (sourceDir.exists() && sourceDir.isDirectory) {
            // 递归复制所有文件
            sourceDir.walkTopDown().forEach { sourceFile ->
                if (sourceFile.isFile && sourceFile.extension == "java") {
                    // 计算相对路径以保持目录结构
                    val relativePath = sourceFile.relativeTo(sourceDir)
                    val targetFile = File(utilDir, relativePath.path)

                    // 确保目标目录存在
                    targetFile.parentFile.mkdirs()

                    // 读取源文件内容并处理package声明
                    val content = sourceFile.readText()
                        .replace("package com.github.mikumiku.addon.v1211;", "package com.github.mikumiku.addon.util;")
                        .replace("package com.github.mikumiku.addon.v1214;", "package com.github.mikumiku.addon.util;")
                        .replace("package com.github.mikumiku.addon.v1218;", "package com.github.mikumiku.addon.util;")
                        .replace("package com.github.mikumiku.addon.v12111;", "package com.github.mikumiku.addon.util;")

                    targetFile.writeText(content)
                    println("Version-specific files have been copied and processed: ${sourceFile.name} -> ${targetFile.absolutePath}")
                }
            }
        } else {
            throw GradleException("The version-specific source directory does not exist: ${sourceDir.absolutePath}")
        }
    }
}

sourceSets {
    main {
        java {
            // 排除原始的版本特定文件
            exclude("**/v1211/**")
            exclude("**/v1214/**")
            exclude("**/v1218/**")
            exclude("**/v12111/**")
        }
    }
}



tasks {
    processResources {

        val versionConstantsFile =
            File(sourceSets.main.get().java.srcDirs.first(), "com/github/mikumiku/addon/VersionConstants.java")
        versionConstantsFile.parentFile.mkdirs()
        versionConstantsFile.writeText(
            """
package com.github.mikumiku.addon;

public final class VersionConstants {
    public static final String MINECRAFT_VERSION = "$currentMcVersion";
    public static final String MIKU_VERSION = "$version";
    public static final boolean IS_1_21_1 = "1.21.1".equals(MINECRAFT_VERSION);
    public static final boolean IS_1_21_4 = "1.21.4".equals(MINECRAFT_VERSION);
}
    """.trimIndent()
        )


        val propertyMap = mapOf(
            "version" to version,
            "mc_version" to prop.getProperty("minecraft_dependency"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        val archiveName = base.archivesName.get()
        from("LICENSE") {
            rename { "${it}_$archiveName" }
        }
        from({
            extraLibs.map { if (it.isDirectory) it else zipTree(it) }
        })
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.release = 21

        dependsOn("prepareVersionSpecificSources")

    }

}

//
//   integration
//
// 3. 定义一个用于注入服务的辅助接口（写在 build.gradle.kts 的末尾或任意位置即可）
interface InjectedExecOps {
    @get:Inject
    val execOps: ExecOperations
}

val execOps = objects.newInstance<InjectedExecOps>().execOps
