plugins {
    id("org.jetbrains.intellij.platform") version "2.3.1"
}

intellijPlatform {
    pluginConfiguration {
        name = "Jay — Coding Agent"
        ideaVersion {
            sinceBuild = "233"
        }
        description = """
            Jay Coding Agent for JetBrains IDEs.
            Provides AI-assisted coding directly within IntelliJ, PyCharm, and other JetBrains IDEs.
        """.trimIndent()
    }
}

dependencies {
    intellijPlatform {
        intellijIdeaCommunity("2024.1")
    }
}
