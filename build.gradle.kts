import org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper

plugins {
    id("com.github.ben-manes.versions") version "0.42.0"
    kotlin("jvm") version "1.6.20" apply false
    id("org.jetbrains.dokka") version "1.6.20" apply false
}

subprojects {

    repositories {
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