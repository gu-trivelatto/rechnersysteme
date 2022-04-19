
plugins {
    kotlin("jvm")
    application
}

application {
    mainClass.set("rs.rs2.cgra.app.MainKt")
}

version = "1.0"
group = "rs.rs2.cgra"

val disasmVersion: String by project
val cgraSimVersion: String by project
val jcomUtilVersion: String by project
val kotlinUtilVersion: String by project
val logbackVersion: String by project
val resourceModelVersion: String by project
val slfjVersion: String by project
val simFrameworkVersion: String by project
val synthesisVersion: String by project
val withSynthesisVersion: String by project

dependencies {

    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    implementation("de.tu_darmstadt.rs.util:kotlin:$kotlinUtilVersion")
    implementation("de.tu_darmstadt.rs.util:jcommander-util:$jcomUtilVersion")

    implementation("de.tu_darmstadt.rs.simulator:framework:$simFrameworkVersion")

    implementation("de.tu_darmstadt.rs.cgra:CGRA:$resourceModelVersion")
    implementation("de.tu_darmstadt.rs.cgra:simulator-impl:$cgraSimVersion")
    implementation(testFixtures("de.tu_darmstadt.rs.cgra:simulator-impl:$cgraSimVersion"))
    implementation("de.tu_darmstadt.rs.cgra:synthesis-debugging:$synthesisVersion")
    implementation(testFixtures("de.tu_darmstadt.rs.cgra:synthesis-debugging:$synthesisVersion"))

    implementation("org.slf4j:slf4j-api:$slfjVersion")
    implementation("ch.qos.logback:logback-classic:$logbackVersion")

//    implementation("de.tu_darmstadt.rs.risc-v:disasm:$disasmVersion")
    implementation("de.tu_darmstadt.rs.risc-v:withSynthesis:$withSynthesisVersion")
}

class Uniquify() {
    private val mapper = mutableMapOf<String, Int>()

    fun remap(fileName: String): String {
        val prev = mapper[fileName]
        val suffix = if (prev != null) {
            val uniqueId = prev + 1
            mapper[fileName] = uniqueId
            "-$uniqueId"
        } else {
            mapper[fileName] = 0
            ""
        }
        val repl = Regex("(.*)\\.jar").replace(fileName, "$1$suffix.jar")
        return repl
    }

    fun remap(file: File): String {
        return remap(file.name)
    }
}

tasks {
    startScripts {
        val remapper = Uniquify()
        val files = (jar.get().outputs.files + configurations.runtimeClasspath.get().files).map {
            remapper.remap(it)
        }
        classpath = files(files)
    }
    test {
        useJUnitPlatform {
            excludeTags("slow")
        }
    }
}

distributions {
    main {
        contents {
            val remapper = Uniquify()
            duplicatesStrategy = DuplicatesStrategy.WARN
            rename {
                remapper.remap(it)
            }
        }

    }
}