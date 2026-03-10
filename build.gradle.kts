import org.apache.tools.ant.filters.EscapeUnicode
import org.intellij.markdown.flavours.commonmark.CommonMarkFlavourDescriptor
import org.intellij.markdown.html.HtmlGenerator
import org.intellij.markdown.parser.MarkdownParser
import org.jetbrains.changelog.Changelog
import org.jetbrains.changelog.markdownToHTML
import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

plugins {
    id("java") // Java support
    alias(libs.plugins.kotlin) // Kotlin support
    alias(libs.plugins.changelog) // Gradle Changelog Plugin
    id("org.jetbrains.intellij.platform")
//    alias(libs.plugins.gradleIntelliJPlugin) // Gradle IntelliJ Plugin
}
kotlin {
    jvmToolchain(17) // 使用 JDK 17
}

fun properties(key: String) = providers.gradleProperty(key)
fun environment(key: String) = providers.environmentVariable(key)
fun dateValue(pattern: String): String =
    LocalDate.now(ZoneId.of("Asia/Shanghai")).format(DateTimeFormatter.ofPattern(pattern))

group = properties("pluginGroup").get()
version = properties("pluginVersion").get()

//repositories {
//    mavenCentral()
//    maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
//}
val embedded by configurations.creating
dependencies {
    intellijPlatform {
//        intellijIdea("2025.3.3")
        bundledPlugin("com.intellij.java")
//        plugin("org.intellij.scala", "2024.1.4")
//        pluginName = properties("pluginName")
        val type = properties("platformType")
        val ver = properties("platformVersion")
        create(type, ver)
//        create(IntelliJPlatformType.IntellijIdea, version)
    }
    implementation(libs.jsoup)
    implementation(libs.zxingcore)
    implementation(libs.zxingjavase)
    implementation(libs.rsyntaxtextarea)
    implementation(libs.hutool)
    implementation(libs.jdom2)
    implementation(libs.markdown)
}

//intellij {
//    pluginName = properties("pluginName")
//    version = properties("platformVersion")
//    type = properties("platformType")
//    plugins = properties("platformPlugins").map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
//}
intellijPlatform {
    pluginConfiguration {
        name = properties("pluginName")
        version = properties("pluginVersion")
        // Extract the <!-- Plugin description --> section from README.md and provide for the plugin's manifest
        val flavour = CommonMarkFlavourDescriptor()
        val parser = MarkdownParser(flavour)
        val markdownText = projectDir.resolve("DESCRIPTION.md").readText()
        val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
        val html = HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
        description = html
//        description = providers.fileContents(layout.projectDirectory.file("README.md")).asText.map {
//            val start = "<!-- Plugin description -->"
//            val end = "<!-- Plugin description end -->"
//
//            with(it.lines()) {
//                if (!containsAll(listOf(start, end))) {
//                    throw GradleException("Plugin description section not found in README.md:\n$start ... $end")
//                }
//                subList(indexOf(start) + 1, indexOf(end)).joinToString("\n").let(::markdownToHTML)
//            }
//        }

        // Get the latest available change notes from the changelog file
        changeNotes = provider {
            val markdownText = projectDir.resolve("CHANGELOG.md").readText()
            val lines = markdownText.split("\n")
            var htmlContent = ""
            var inList = false

            for (line in lines) {
                when {
                    line.matches(Regex("^# (.+)$")) -> {
                        if (inList) htmlContent += "</ul>"
                        htmlContent += "<h1>${line.removePrefix("# ")}</h1>"
                        inList = false
                    }

                    line.matches(Regex("^## (.+)$")) -> {
                        if (inList) htmlContent += "</ul>"
                        htmlContent += "<h2>${line.removePrefix("## ")}</h2>"
                        inList = false
                    }

                    line.matches(Regex("^### (.+)$")) -> {
                        if (inList) htmlContent += "</ul>"
                        htmlContent += "<h3>${line.removePrefix("### ")}</h3>"
                        inList = false
                    }

                    line.matches(Regex("^- (.+)$")) -> {
                        if (!inList) {
                            htmlContent += "<ul>"
                            inList = true
                        }
                        htmlContent += "<li>${line.removePrefix("- ")}</li>"
                    }

                    inList && (line.isBlank() || line.matches(Regex("^#.+|^##.+|^###.+"))) -> {
                        htmlContent += "</ul>"
                        inList = false
                        if (line.isNotBlank()) htmlContent += "<br>"
                    }

                    else -> {
                        if (inList) htmlContent += "</ul>"
                        inList = false
                        htmlContent += if (line.isBlank()) "<br>" else "$line<br>"
                    }
                }
            }
            if (inList) htmlContent += "</ul>"
            htmlContent
        }
//        changeNotes = providers.gradleProperty("pluginVersion").map { pluginVersion ->
//            with(changelog) {
//                renderItem(
//                    (getOrNull(pluginVersion) ?: getUnreleased())
//                        .withHeader(false)
//                        .withEmptySections(false),
//                    Changelog.OutputType.HTML,
//                )
//            }
//        }

        ideaVersion {
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
        }
    }

    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
        // The pluginVersion is based on the SemVer (https://semver.org) and supports pre-release labels, like 2.1.7-alpha.3
        // Specify pre-release label to publish the plugin in a custom Release Channel automatically. Read more:
        // https://plugins.jetbrains.com/docs/intellij/publishing-plugin.html#specifying-a-release-channel
        channels = providers.gradleProperty("pluginVersion")
            .map { listOf(it.substringAfter('-', "").substringBefore('.').ifEmpty { "default" }) }
    }

    pluginVerification {
        ides {
            recommended()
        }
    }
}
changelog {
    header = provider { "${version.get()} (${dateValue("yyyy/MM/dd")})" }
    groups.empty()
    repositoryUrl = properties("pluginRepositoryUrl")
    versionPrefix = ""
}
tasks {
//    patchPluginXml {
//        version = properties("pluginVersion")
//        sinceBuild = properties("pluginSinceBuild")
//        untilBuild = properties("pluginUntilBuild")
//
//        val flavour = CommonMarkFlavourDescriptor()
//        val parser = MarkdownParser(flavour)
//        val markdownText = projectDir.resolve("DESCRIPTION.md").readText()
//        val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
//        val html = HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
//        pluginDescription.set(html)
//
//
//    }
    wrapper {
        gradleVersion = providers.gradleProperty("gradleVersion").get()
    }

    publishPlugin {
        dependsOn(patchChangelog)
    }
}
//tasks {
//    patchPluginXml {
//        version = properties("pluginVersion")
//        sinceBuild = properties("pluginSinceBuild")
//        untilBuild = properties("pluginUntilBuild")
//
//        val flavour = CommonMarkFlavourDescriptor()
//        val parser = MarkdownParser(flavour)
//        val markdownText = projectDir.resolve("DESCRIPTION.md").readText()
//        val parsedTree = parser.buildMarkdownTreeFromString(markdownText)
//        val html = HtmlGenerator(markdownText, parsedTree, flavour).generateHtml()
//        pluginDescription.set(html)
//
//
//    }
////    listProductsReleases {
////        sinceVersion = "2024.1"
////    }
//    signPlugin {
//        certificateChain = environment("CERTIFICATE_CHAIN")
//        privateKey = environment("PRIVATE_KEY")
//        password = environment("PRIVATE_KEY_PASSWORD")
//    }
//    publishPlugin {
//        dependsOn("patchChangelog")
//        token = environment("PUBLISH_TOKEN")
//    }
//    wrapper {
//        gradleVersion = properties("gradleVersion").get()
//        distributionType = Wrapper.DistributionType.ALL
//    }
//    processResources {
//        filesMatching("**/*.properties") {
//            filter(EscapeUnicode::class)
//        }
//    }
//}
