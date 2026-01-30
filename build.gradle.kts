plugins {
  java
  id("net.minecraftforge.gradle") version "5.1.+"
  id("org.spongepowered.mixin") version "0.7.+"
}

val minecraftVersion: String = "1.18.2"
val forgeVersion: String = "40.1.73"
val tfcVersion: String = "2.2.33"

val modId: String = "tfc_real_world"
val modVersion: String = System.getenv("VERSION") ?: "0.0.0-indev"
val modJavaVersion: String = "17"

val generateModMetadata = tasks.register<ProcessResources>("generateModMetadata") {
  val modReplacementProperties = mapOf(
    "modId" to modId,
    "modVersion" to modVersion,
    "minecraftVersionRange" to "[$minecraftVersion]",
    "forgeVersionRange" to "[$forgeVersion,)",
    "tfcVersionRange" to "[$tfcVersion]",
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

  // TerraFirmaCraft
  compileOnly(fg.deobf("net.dries007.tfc:TerraFirmaCraft-Forge-$minecraftVersion:$tfcVersion@jar"))

  // Mixin
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
}
