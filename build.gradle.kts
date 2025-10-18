import java.util.*

plugins {
    id("fabric-loom") version "1.11-SNAPSHOT"
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

}

// Configuration that holds jars to include in the jar
val extraLibs: Configuration by configurations.creating

dependencies {
    minecraft("com.mojang:minecraft:${currentMcVersion}")
    mappings("net.fabricmc:yarn:${prop.getProperty("yarn_mappings")}:v2")
    modImplementation("net.fabricmc:fabric-loader:${prop.getProperty("loader_version")}")

    // 使用配置中定义的meteor版本
    modImplementation("meteordevelopment:meteor-client:${prop.getProperty("meteor_version")}")

    // XaeroPlus https://modrinth.com/mod/xaeroplus/version/2.27.2+fabric-1.21.1
    modImplementation("maven.modrinth:xaeroplus:${prop.getProperty("xaeroplus_version")}")
    // XaeroWorldMap https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21
    //               https://modrinth.com/mod/xaeros-world-map/version/1.39.12_Fabric_1.21.4
    modImplementation("maven.modrinth:xaeros-world-map:${prop.getProperty("xaeros_worldmap_version")}")
    // XaeroMinimap https://modrinth.com/mod/xaeros-minimap/version/25.2.10_Fabric_1.21
    modImplementation("maven.modrinth:xaeros-minimap:${prop.getProperty("xaeros_minimap_version")}")
//    modImplementation("maven.modrinth:litematica:${prop.getProperty("litematica_version")}")
    modCompileOnly("meteordevelopment:baritone:${prop.getProperty("baritone_version")}")

    modImplementation("com.github.sakura-ryoko:malilib:${prop.getProperty("malilib_version")}")
    modImplementation("com.github.sakura-ryoko:litematica:${prop.getProperty("litematica_version")}")

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

sourceSets {
    main {
        java {
            val currentVersion = currentMcVersion.replace(".", "").toInt()
            val implDir = File(sourceSets.main.get().java.srcDirs.first(), "com/github/mikumiku/addon/impl")
            println("all implDirs => ${implDir.list().contentToString()}")
            val verMap = mutableMapOf<String, MutableList<Int>>()
            val verFileMap = mutableMapOf<String, String>()
            for (vPath in implDir.listFiles()) {
                if (vPath.exists() && vPath.isDirectory) {
                    val versionNumber = vPath.name.replace("v", "").toInt()
                    vPath.walkTopDown().forEach { javaFile ->
                        if (javaFile.isFile && javaFile.extension == "java") {
                            var vs = verMap[javaFile.name]
                            if (vs == null) {
                                vs = mutableListOf()
                                verMap[javaFile.name] = vs
                            }
                            vs.add(versionNumber)
                            verFileMap[javaFile.name + versionNumber] = javaFile.path
                        }
                    }
                }
            }

            // 选择最佳版本，其他版本排除
            verMap.forEach { (name, vs) ->
                var useVer: Int = -1
                vs.sortByDescending { it -> it }
                vs.forEach { ver ->
                    if (useVer == -1 && ver <= currentVersion) {
                        useVer = ver
                    } else {
                        var fullJavaPath = verFileMap[name + ver]
                        fullJavaPath = fullJavaPath?.substring(fullJavaPath.indexOf("\\src\\main\\java\\") + 15, fullJavaPath.length)
                        fullJavaPath = fullJavaPath?.replace("\\", "/")
                        exclude(fullJavaPath)
                        println("exclude => $fullJavaPath")
                    }
                }
            }
        }
    }
}



tasks {
    processResources {

//        val versionConstantsFile =
//            File(sourceSets.main.get().java.srcDirs.first(), "com/github/mikumiku/addon/VersionConstants.java")
//        versionConstantsFile.parentFile.mkdirs()
//        versionConstantsFile.writeText(
//            """
//package com.github.mikumiku.addon;
//
//public final class VersionConstants {
//    public static final String MINECRAFT_VERSION = "$currentMcVersion";
//}
//    """.trimIndent()
//        )


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

    }
}
