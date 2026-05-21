plugins {
  java
  id("net.minecraftforge.gradle") version "[6.0,6.2)"
  id("org.spongepowered.mixin") version "0.7.+"
}

val minecraftVersion: String = "1.20.1"
val forgeVersion: String = "47.1.3"
val minTfcVersion: String = "3.2.20"
val maxTfcVersion: String = "3.2.22"

val modId: String = "tfc_real_world"
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"
val modJavaVersion: String = "17"

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
  val modReplacementProperties = mapOf(
    "modId" to modId,
    "modVersion" to modVersion,
    "minecraftVersionRange" to "[$minecraftVersion]",
    "forgeVersionRange" to "[$forgeVersion,)",
    "tfcVersionRange" to "[$minTfcVersion,)",
  )
  inputs.properties(modReplacementProperties)
  expand(modReplacementProperties)
  from("src/main/templates")
  into(layout.buildDirectory.dir("generated/sources/modMetadata"))
}

base {
  archivesName.set("TFC-Real-World-Forge-$minecraftVersion")
  group = "net.yazloysasha.tfcrealworld"
  version = modVersion
}

java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(modJavaVersion))
}

repositories {
  mavenCentral()
  mavenLocal()
  maven(url = "https://www.cursemaven.com") {
    content {
      includeGroup("curse.maven")
    }
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
  main {
    resources {
      srcDir(generateModMetadata)
    }
  }
}

minecraft {
  mappings("official", minecraftVersion)

  runs {
    all {
      args("-mixin.config=$modId.mixins.json")
      
      property("forge.logging.console.level", "debug")
      property("forge.enabledGameTestNamespaces", modId)
      
      property("mixin.env.remapRefMap", "true")
      property("mixin.env.refMapRemappingFile", "$projectDir/build/createSrgToMcp/output.srg")
      
      jvmArgs("-ea", "-Xmx4G", "-Xms4G")
      
      mods.create(modId) {
        source(sourceSets.main.get())
      }
    }

    register("client") {
      workingDirectory(project.file("run/client"))
    }

    register("server") {
      workingDirectory(project.file("run/server"))
      arg("--nogui")
    }
  }
}

mixin {
  add(sourceSets.main.get(), "$modId.refmap.json")
}

dependencies {
  minecraft("net.minecraftforge", "forge", version = "$minecraftVersion-$forgeVersion")

  compileOnly(fg.deobf("net.dries007.tfc:TerraFirmaCraft-Forge-$minecraftVersion:$maxTfcVersion@jar"))

  testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.2")
  testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.2")
  testImplementation(fg.deobf("net.dries007.tfc:TerraFirmaCraft-Forge-$minecraftVersion:$maxTfcVersion@jar"))

  annotationProcessor("org.spongepowered:mixin:0.8.5:processor")
}

tasks {
  processResources {
    dependsOn(generateModMetadata)
  }

  jar {
    manifest {
      attributes["Implementation-Version"] = project.version
      attributes["MixinConfigs"] = "$modId.mixins.json"
    }
  }

  test {
    useJUnitPlatform()
    testLogging {
      events("passed", "failed", "skipped", "standardOut", "standardError")
      showStandardStreams = true
      showStackTraces = true
      exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
  }
}
