import groovy.json.JsonOutput
import groovy.json.JsonSlurper

plugins {
  id("net.neoforged.moddev") version "2.0.107"
}

val minecraftVersion: String = "1.21.1"
val neoForgeVersion: String = "21.1.234"
val patchouliVersion: String = "1.21.1-92-NEOFORGE"
val minTfcVersion: String = "4.2.0"
val maxTfcVersion: String = "4.2.10"

val modId: String = "tfc_real_world"
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"
val modJavaVersion: String = "21"

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
  val modReplacementProperties = mapOf(
    "modId" to modId,
    "modVersion" to modVersion,
    "minecraftVersionRange" to "[$minecraftVersion]",
    "neoForgeVersionRange" to "[$neoForgeVersion,)",
    "tfcVersionRange" to "[$minTfcVersion,)",
  )
  inputs.properties(modReplacementProperties)
  expand(modReplacementProperties)
  from("src/main/templates")
  into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

neoForge {
  version = neoForgeVersion
}

base {
  archivesName.set("TFC-Real-World-NeoForge-$minecraftVersion")
  group = "net.yazloysasha.tfcrealworld"
  version = modVersion
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
  mavenCentral()
  mavenLocal()
  exclusiveContent {
    forRepository { maven("https://maven.blamejared.com") }
    filter { includeGroup("vazkii.patchouli") }
  }
  ivy {
    url = uri("https://github.com/TerraFirmaCraft/TerraFirmaCraft/releases/download")
    patternLayout {
      artifact("/v[revision]/[artifact]-[revision].[ext]")
    }
    metadataSources {
      artifact()
    }
  }
}

sourceSets {
  create("stub") {
    java.srcDirs("src/stub/auroras/java")
    compileClasspath += sourceSets["main"].compileClasspath
  }

  main {
    compileClasspath += sourceSets["stub"].output
    resources {
      srcDir(generateModMetadata)
    }
  }
}

neoForge {
  validateAccessTransformers = true

  runs {
    configureEach {
      jvmArguments.addAll("-XX:+IgnoreUnrecognizedVMOptions", "-XX:+AllowEnhancedClassRedefinition", "-ea")
    }
    register("client") {
      client()
      gameDirectory = file("run/client")
    }
    register("server") {
      server()
      gameDirectory = file("run/server")
      programArgument("--nogui")
    }
  }

  mods {
    create(modId) {
      sourceSet(sourceSets.main.get())
    }
  }

  unitTest {
    enable()
    testedMod = mods[modId]
  }

  ideSyncTask(generateModMetadata)
}

dependencies {
  compileOnly("net.dries007.tfc:TerraFirmaCraft-NeoForge-$minecraftVersion:$maxTfcVersion@jar")

  testImplementation("net.dries007.tfc:TerraFirmaCraft-NeoForge-$minecraftVersion:$maxTfcVersion@jar")
  testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
  testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.3")
  testImplementation("vazkii.patchouli:Patchouli:$patchouliVersion")
}

tasks {
  named<ProcessResources>("processResources") {
    doLast {
      val jsonSlurper = JsonSlurper()
      destinationDir.walkTopDown()
        .filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
        .forEach { file ->
          @Suppress("UNCHECKED_CAST")
          val parsed = jsonSlurper.parse(file) as Any
          file.writeText(JsonOutput.toJson(parsed))
        }
    }
  }

  jar {
    exclude("auroras/**")
    manifest {
      attributes["Implementation-Version"] = project.version
    }
  }

  named<JavaCompile>("compileJava") {
    dependsOn("compileStubJava")
  }

  named("neoForgeIdeSync") {
    dependsOn(generateModMetadata)
  }

  test {
    useJUnitPlatform()
    if (project.hasProperty("continuousBiomeCoverage")) {
      systemProperty("continuousBiomeCoverage", "true")
    }
    maxHeapSize = "4g"
    minHeapSize = "1g"
    outputs.upToDateWhen { false }
    testLogging {
      events("failed", "standardError", "standardOut")
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
      showCauses = true
      showExceptions = true
      showStackTraces = true
    }
  }
}
