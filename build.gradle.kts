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
        "loader" to "0.16.5"
    ),
    "1.21.4" to mapOf(
        "minecraft" to "1.21.4",
        "yarn" to "1.21.4+build.1",
        "meteor" to "1.21.4-SNAPSHOT",
        "loader" to "0.16.5"
    )
)

// 获取当前构建的版本（从命令行参数或默认值）
val currentMinecraftVersion = project.findProperty("minecraft_version")?.toString() ?: properties["minecraft_version"].toString()
val currentVersionConfig = versions[currentMinecraftVersion] ?: versions["1.21.1"]!!

// 动态设置版本相关的属性
val dynamicMinecraftVersion = currentVersionConfig["minecraft"]!!
val dynamicYarnMappings = currentVersionConfig["yarn"]!!
val dynamicMeteorVersion = currentVersionConfig["meteor"]!!
val dynamicLoaderVersion = currentVersionConfig["loader"]!!

base {
    archivesName = "${properties["archives_base_name"] as String}-$currentMinecraftVersion"
    version = properties["mod_version"] as String
    group = properties["maven_group"] as String
}

repositories {
    maven {
        name = "meteor-maven"
        url = uri("https://maven.meteordev.org/releases")
    }
    maven {
        name = "meteor-maven-snapshots"
        url = uri("https://maven.meteordev.org/snapshots")
    }

    maven {
        name = "maven3"
        url = uri("https://maven.seedfinding.com/")
    }
}

dependencies {
    minecraft("com.mojang:minecraft:$dynamicMinecraftVersion")
    mappings("net.fabricmc:yarn:$dynamicYarnMappings:v2")
    modImplementation("net.fabricmc:fabric-loader:$dynamicLoaderVersion")

    // 使用配置中定义的meteor版本
    modImplementation("meteordevelopment:meteor-client:$dynamicMeteorVersion")

    modCompileOnly("meteordevelopment:baritone:$dynamicMinecraftVersion-SNAPSHOT")
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
            commandLine("cmd", "/c", "gradlew.bat", "clean", "build", "-x", "test", "--no-configuration-cache", "-Pminecraft_version=$versionName")
        } else {
            commandLine("./gradlew", "clean", "build", "-x", "test", "--no-configuration-cache", "-Pminecraft_version=$versionName")
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

tasks {
    processResources {
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
