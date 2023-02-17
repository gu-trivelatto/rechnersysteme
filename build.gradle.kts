import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    id("com.github.ben-manes.versions") version "0.45.0"
    kotlin("jvm") version "1.8.0" apply false
    id("org.jetbrains.dokka") version "1.7.20" apply false
}

tasks {
    create<Zip>("abgabe") {
        group = "RS2"
        description = "Die Abgabe fuer Moodle in abgabe.zip packen. Enthält nur Dateien der Modifikation für die Abgabe erlaubt ist"

        archiveFileName.set("abgabe.zip")
        destinationDirectory.set(layout.projectDirectory)

        from("gpsAcquisition") {
            into("gpsAcquition")
            exclude("build")
            exclude("main.c")
            exclude("acquisition.h")
            exclude("testData.*")
            exclude("GNU-RISCV-RV32G-newlib-Toolchain.cmake")
            exclude("shared.additional.cmake")
        }


        from("rs2runner/src/main/kotlin/rs/rs2/cgra") {
            into("rs2runner/src/main/kotlin/rs/rs2/cgra")
            include("cgraConfigurations/EnergyFocused.kt")
            include("cgraConfigurations/PerformanceFocused.kt")
            include("optConfig/optConfig.kt")
        }
    }
}

subprojects {

    repositories {
//        mavenLocal()
        mavenCentral()
        maven {
            name = "RS Release"
            url = uri("https://artifactory.rs.e-technik.tu-darmstadt.de/artifactory/gradle-release")
        }
    }

    tasks.withType<JacocoReport>().configureEach {
        doLast {
            logger.lifecycle("##teamcity[jacocoReport dataPath='$buildDir/jacoco/test.exec' includes='de.tu_darmstadt.rs.*']")
        }
    }

    tasks.withType<Test>().configureEach {
        outputs.cacheIf { false }
        maxHeapSize = "2G"
    }

    tasks.withType<Javadoc>().configureEach {
        options.quiet()
    }

    plugins.whenPluginAdded {
        when (this) {
            is JavaBasePlugin -> {
                configure<JavaPluginExtension> {
                    toolchain {
                        languageVersion.set(JavaLanguageVersion.of(17))
                    }
                }

                tasks.withType<JavaCompile>().configureEach {
                    options.encoding = "UTF-8"
                }
            }
            is KotlinPluginWrapper -> {
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                    kotlinOptions {
                        jvmTarget = "17"
                        freeCompilerArgs = listOf("-Xjvm-default=all")
                    }
                }
            }
            is JacocoPlugin -> {
                configure<JacocoPluginExtension> {
                    toolVersion = "0.8.7"
                }
            }
        }
    }
}