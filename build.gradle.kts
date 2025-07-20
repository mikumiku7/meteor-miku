plugins {
    id("fabric-loom") version "1.10-SNAPSHOT"

    id("io.freefair.lombok") version "8.14"
}

// 定义版本配置
val versions = mapOf(
    "1.21.1" to mapOf(
        "minecraft" to "1.21.1",
        "yarn" to "1.21.1+build.3",
        "meteor" to "0.5.8-SNAPSHOT"
    ),
    "1.21.4" to mapOf(
        "minecraft" to "1.21.4",
        "yarn" to "1.21.4+build.1",
        "meteor" to "1.21.4-SNAPSHOT"
    )
)

base {
    archivesName = "${properties["archives_base_name"] as String}-${properties["minecraft_version"] as String}"
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
    // Fabric
    minecraft("com.mojang:minecraft:${properties["minecraft_version"] as String}")
    mappings("net.fabricmc:yarn:${properties["yarn_mappings"] as String}:v2")
    modImplementation("net.fabricmc:fabric-loader:${properties["loader_version"] as String}")

    // 根据版本选择不同的meteor依赖
    val mcVersion = properties["minecraft_version"] as String
    if (mcVersion == "1.21.1") {
    modImplementation("meteordevelopment:meteor-client:${properties["meteor_version"] as String}")
    } else {
        modImplementation("meteordevelopment:meteor-client:${mcVersion}-SNAPSHOT")
    }

    modCompileOnly("meteordevelopment:baritone:${properties["minecraft_version"] as String}-SNAPSHOT")
//    compileOnly("org.projectlombok:lombok:1.18.38")
//    annotationProcessor("org.projectlombok:lombok:1.18.38")

}

// 创建多版本构建任务
versions.forEach { (versionName, config) ->
    val taskSuffix = versionName.replace(".", "")

    // 创建专用的jar任务
    val jarTask = tasks.register<Jar>("jar$taskSuffix") {
        group = "build"
        description = "Build jar for Minecraft $versionName"

        archiveBaseName.set("${properties["archives_base_name"]}-${config["minecraft"]}")
        archiveVersion.set(properties["mod_version"] as String)

        from(sourceSets.main.get().output)

        from("LICENSE") {
            rename { "${it}_${archiveBaseName.get()}" }
        }

        // 动态设置依赖配置
        doFirst {
            // 这里可以添加版本特定的处理逻辑
        }
    }

    // 创建构建任务
    tasks.register("build$taskSuffix") {
        group = "build"
        description = "Build for Minecraft $versionName"
        dependsOn(jarTask)
    }
}

// 构建所有版本的任务
tasks.register("buildAll") {
    group = "build"
    description = "Build all versions"
    dependsOn("build1211", "build1214")
}

tasks {
    processResources {
        val propertyMap = mapOf(
            "version" to project.version,
            "mc_version" to project.property("minecraft_version"),
        )

        inputs.properties(propertyMap)

        filteringCharset = "UTF-8"

        filesMatching("fabric.mod.json") {
            expand(propertyMap)
        }
    }

    jar {
        inputs.property("archivesName", project.base.archivesName.get())

        from("LICENSE") {
            rename { "${it}_${inputs.properties["archivesName"]}" }
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
