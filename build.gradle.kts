plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
    `maven-publish`
}

group = "com.github.ninebolt6"
version = "0.0.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(platform(libs.junit.bom))
    implementation(libs.junit.jupiter.api)
    implementation(libs.mockk)

    testImplementation(libs.junit.jupiter)
    testImplementation(libs.junit.platform.testkit)
}

java {
    withSourcesJar()
    withJavadocJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
            pom {
                name = "MockAuditExtension"
                description = "JUnit 5 Extension for MockK mock verification - detects unused stubs and unverified mock calls"
                url = "https://github.com/ninebolt6/mock-audit-extension"

                licenses {
                    license {
                        name = "Eclipse Public License 2.0"
                        url = "https://www.eclipse.org/legal/epl-2.0/"
                    }
                }

                developers {
                    developer {
                        id = "ninebolt6"
                        name = "ninebolt6"
                        url = "https://github.com/ninebolt6"
                    }
                }

                scm {
                    connection = "scm:git:git://github.com/ninebolt6/mock-audit-extension.git"
                    developerConnection = "scm:git:ssh://github.com/ninebolt6/mock-audit-extension.git"
                    url = "https://github.com/ninebolt6/mock-audit-extension"
                }
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
