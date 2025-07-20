plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"
    id("io.freefair.lombok") version "8.14"
}

// 定义版本配置
val versions = mapOf(
    "1.21.1" to mapOf(
        "minecraft" to "1.21.1",
        "yarn" to "1.21.1+build.3",
        "meteor" to "0.5.8-SNAPSHOT",
        "loader" to "0.16.5",
        "xaeroplus_version" to "2.27.2+fabric-1.21.1",
        "xaeros_worldmap_version" to "1.39.12_Fabric_1.21",
        "xaeros_minimap_version" to "25.2.10_Fabric_1.21"
    ),
    "1.21.4" to mapOf(
        "minecraft" to "1.21.4",
        "yarn" to "1.21.4+build.1",
        "meteor" to "1.21.4-SNAPSHOT",
        "loader" to "0.16.5",
        "xaeroplus_version" to "2.27.2+fabric-1.21.4",
        "xaeros_worldmap_version" to "1.39.12_Fabric_1.21.4",
        "xaeros_minimap_version" to "25.2.10_Fabric_1.21.4"
    )
)

// 获取当前构建的版本（从命令行参数或默认值）
val currentMinecraftVersion =
    project.findProperty("minecraft_version")?.toString() ?: properties["minecraft_version"].toString()
val currentVersionConfig = versions[currentMinecraftVersion] ?: versions["1.21.1"]!!

// 动态设置版本相关的属性
val dynamicMinecraftVersion = currentVersionConfig["minecraft"]!!
val dynamicYarnMappings = currentVersionConfig["yarn"]!!
val dynamicMeteorVersion = currentVersionConfig["meteor"]!!
val dynamicLoaderVersion = currentVersionConfig["loader"]!!
val xaeroplus_version = currentVersionConfig["xaeroplus_version"]!!
val xaeros_worldmap_version = currentVersionConfig["xaeros_worldmap_version"]!!
val xaeros_minimap_version = currentVersionConfig["xaeros_minimap_version"]!!

base {
    archivesName = "${properties["archives_base_name"] as String}-$currentMinecraftVersion"
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
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
    maven { url = uri("https://jitpack.io") }
    maven { url = uri("https://maven.duti.dev/releases") }
}

// Configuration that holds jars to include in the jar
val extraLibs: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:$dynamicMinecraftVersion")
    mappings("net.fabricmc:yarn:$dynamicYarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$dynamicLoaderVersion")

    // 使用配置中定义的meteor版本
    modImplementation("meteordevelopment:meteor-client:$dynamicMeteorVersion")

    // XaeroPlus https://modrinth.com/mod/xaeroplus/version/2.27.2+fabric-1.21.1
    modImplementation("maven.modrinth:xaeroplus:${xaeroplus_version}")
    // XaeroWorldMap https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21
    //               https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21.4
    modImplementation("maven.modrinth:xaeros-world-map:${xaeros_worldmap_version}")
    // XaeroMinimap https://modrinth.com/mod/xaeros-minimap/version/25.2.10_Fabric_1.21
    modImplementation("maven.modrinth:xaeros-minimap:${xaeros_minimap_version}")

    modCompileOnly("meteordevelopment:baritone:$dynamicMinecraftVersion-SNAPSHOT")



    extraLibs("dev.duti.acheong:cubiomes:1.22.5") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:linux64") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:osx") { isTransitive = false }
    extraLibs("dev.duti.acheong:cubiomes:1.22.5:windows64") { isTransitive = false }

    extraLibs("com.seedfinding:mc_core:1.210.0") { isTransitive = false }
    extraLibs("com.seedfinding:mc_math:1.171.0") { isTransitive = false }
    extraLibs("com.seedfinding:mc_seed:1.171.2") { isTransitive = false }
    extraLibs("com.seedfinding:mc_noise:1.171.1") { isTransitive = false }
    extraLibs("com.seedfinding:mc_biome:1.171.1") { isTransitive = false }
    extraLibs("com.seedfinding:mc_terrain:1.171.1") { isTransitive = false }
    extraLibs("com.seedfinding:mc_feature:1.171.10") { isTransitive = false }

    configurations.implementation.get().extendsFrom(extraLibs)

}

// 为每个版本创建构建任务
versions.forEach { (versionName, config) ->
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

    dependsOn(versions.keys.map { "buildFor${it.replace(".", "")}" })
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

// 在编译前生成版本特定类
tasks.register("prepareVersionSpecificSources") {
    doLast {
        val utilDir = file("src/main/java/com/github/mikumiku/addon/util")
        utilDir.mkdirs()

        val sourceFile = when (currentMinecraftVersion) {
            "1.21.1" -> file("src/main/java/com/github/mikumiku/addon/v1211/Ore.java")
            "1.21.4" -> file("src/main/java/com/github/mikumiku/addon/v1214/Ore.java")
            else -> throw GradleException("不支持的版本: $currentMinecraftVersion")
        }

        if (sourceFile.exists()) {
            val targetFile = File(utilDir, "Ore.java")
            val content = sourceFile.readText()
                .replace("package com.github.mikumiku.addon.v1211;", "package com.github.mikumiku.addon.util;")
                .replace("package com.github.mikumiku.addon.v1214;", "package com.github.mikumiku.addon.util;")

            targetFile.writeText(content)
            println("已生成版本特定类: ${targetFile.absolutePath}")
        }
    }
}

sourceSets {
    main {
        java {
            // 排除原始的版本特定文件
            exclude("**/v1211/**")
            exclude("**/v1214/**")
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
    public static final String MINECRAFT_VERSION = "$currentMinecraftVersion";
    public static final boolean IS_1_21_1 = "1.21.1".equals(MINECRAFT_VERSION);
    public static final boolean IS_1_21_4 = "1.21.4".equals(MINECRAFT_VERSION);
}
    """.trimIndent()
        )


        val propertyMap = mapOf(
            "version" to version,
            "mc_version" to currentMinecraftVersion,
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
