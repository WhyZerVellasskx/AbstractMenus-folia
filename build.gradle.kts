
@file:Suppress("VulnerableLibrariesLocal")

import kotlin.io.path.listDirectoryEntries
import io.papermc.paperweight.userdev.ReobfArtifactConfiguration

plugins {
    java
    kotlin("jvm") version "2.1.20"
    id("com.gradleup.shadow") version "9.6.1"
    id("io.papermc.paperweight.userdev") version "1.7.7"
    id("de.eldoria.plugin-yml.bukkit") version "0.7.1"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "ru.abstractmenus"
version = "1.18.0-alpha"

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

kotlin {
    jvmToolchain(21)
}

paperweight.reobfArtifactConfiguration = ReobfArtifactConfiguration.MOJANG_PRODUCTION

repositories {
    mavenLocal()
    mavenCentral()

    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/")
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven("https://maven.enginehub.org/repo/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://mvn.lumine.io/repository/maven-public/")
    maven("https://maven.citizensnpcs.co/repo")
    maven("https://nexus.phoenixdevt.fr/repository/maven-public/")

    maven("https://repo.codemc.org/repository/maven-public/") {
        metadataSources { artifact() }
    }

    maven("https://repo.codemc.org/repository/maven-snapshots/") {
        metadataSources { artifact() }
    }

    maven("https://repo.codemc.org/repository/maven-releases/") {
        metadataSources { artifact() }
    }

    flatDir { dirs("gradle/libs") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    paperweight.paperDevBundle("1.21-R0.1-SNAPSHOT")

    val lombokVersion = "1.18.32"
    compileOnly("org.projectlombok:lombok:$lombokVersion")
    annotationProcessor("org.projectlombok:lombok:$lombokVersion")
    testCompileOnly("org.projectlombok:lombok:$lombokVersion")
    testAnnotationProcessor("org.projectlombok:lombok:$lombokVersion")

    library(kotlin("stdlib"))
    library(kotlin("reflect"))

    implementation("io.github.blackbaroness:duration-serializer:2.0.2")

    implementation("de.tr7zw:item-nbt-api-plugin:2.9.0") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("com.github.MilkBowl:VaultAPI:1.7") {
        exclude(group = "org.bukkit", module = "bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    implementation("com.arcaniax:HeadDatabase-API:1.3.2") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("net.luckperms:api:5.4") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.0") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.bukkit", module = "bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    implementation("io.lumine:MythicLib:1.0.12-SNAPSHOT") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("com.github.LoneDev6:api-itemsadder:3.6.1") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("net.skinsrestorer:skinsrestorer-api:15.3.1") {
        exclude(group = "org.bstats", module = "bstats-bukkit")
        exclude(group = "org.spigotmc", module = "spigot-api")
    }

    compileOnly("net.citizensnpcs:citizens-main:2.0.35-SNAPSHOT") {
        isTransitive = false
    }

    implementation("io.lumine:MythicLib-dist:1.6.2-SNAPSHOT")
    implementation("net.Indyuce:MMOItems-API:6.9.5-SNAPSHOT")
    implementation("org.yaml:snakeyaml:1.22")

    compileOnly("me.clip:placeholderapi:2.11.6")

    val junitVersion = "5.10.2"

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.2")

    implementation("com.github.AbstractMenus:api:995cd8c9a9")
    implementation("com.fathzer:javaluator:3.0.3")
    implementation("com.github.technicallycoded:FoliaLib:0.4.3")
    implementation("org.openjdk.nashorn:nashorn-core:15.6")
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveClassifier.set("plain")
}

val pluginPackage = "ru.abstractmenus"

tasks.shadowJar {
    archiveBaseName.set("AbstractMenus")
    archiveVersion.set(project.version.toString())
    archiveClassifier.set("")
    archiveFileName.set("AbstractMenus-${project.version}.jar")
    destinationDirectory.set(layout.buildDirectory.dir("libs"))

    exclude(
        "DebugProbesKt.bin",
        "LICENSE*",
        "deprecated.properties",
        "driver.properties",
        "mariadb.properties",
        "OSGI-INF/**",
        "META-INF/*.SF",
        "META-INF/*.RSA",
        "META-INF/*.DSA",
        "org/slf4j/**"
    )

    dependencies {
        exclude(dependency("org.jetbrains.kotlin:.*"))
        exclude(dependency("org.jetbrains.kotlinx:.*"))
        exclude(dependency("org.checkerframework:.*"))
        exclude(dependency("org.jetbrains:annotations"))
        exclude(dependency("org.slf4j:.*"))
    }

    manifest {
        attributes["paperweight-mappings-namespace"] = "mojang"
    }

    rootDir.resolve("gradle/relocations.txt")
        .takeIf { it.isFile }
        ?.forEachLine { line ->
            val packageName = line.trim()
            if (packageName.isNotEmpty() && !packageName.startsWith("#")) {
                relocate(packageName, "$pluginPackage.__relocated__.$packageName")
            }
        }
}

tasks.assemble {
    dependsOn(tasks.shadowJar)
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

bukkit {
    name = rootProject.name
    main = "$pluginPackage.AbstractMenus"
    version = project.version.toString()
    apiVersion = "1.20"
    description = project.description
    author = "Nanit, BrainRTP, WhyZerVellasskx"
    website = "https://github.com/AbstractMenus/minecraft-plugin"
    generateLibrariesJson = true
    foliaSupported = true

    softDepend = listOf(
        "Vault",
        "WorldGuard",
        "PlaceholderAPI",
        "NBTAPI",
        "LuckPerms",
        "Citizens",
        "HeadDatabase",
        "MMOItems",
        "SkinsRestorer",
        "ItemsAdder",
        "Oraxen"
    )

    commands {
        val adminPermission = "am.admin"

        register("am") {
            permission = adminPermission
            description = "Menu control commands"
        }

        register("var") {
            permission = adminPermission
            description = "Global variables control commands"
        }

        register("varp") {
            permission = adminPermission
            description = "Personal variables control commands"
        }
    }
}

tasks.runServer {
    dependsOn(tasks.shadowJar)

    minecraftVersion("1.21.3")
    serverJar(rootDir.resolve("gradle/folia.jar"))

    downloadPlugins {
        rootDir.resolve("gradle/server-plugins")
            .takeIf { it.isDirectory }
            ?.also { pluginJars(it.toPath().listDirectoryEntries("*.jar")) }
    }

    @Suppress("USELESS_ELVIS")
    jvmArgs = (jvmArgs ?: listOf())
        .plus("-DPaper.IgnoreJavaVersion=true")
        .plus("-Dfile.encoding=UTF-8")
        .plus("-DIReallyKnowWhatIAmDoingISwear")
        .plus("-Dcom.mojang.eula.agree=true")
}
