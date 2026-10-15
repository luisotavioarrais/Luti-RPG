plugins { java }
group = "br.com.lutiplay"
version = "0.3.4"
java { toolchain { languageVersion.set(JavaLanguageVersion.of(25)) } }
repositories { mavenCentral() }
dependencies {
    // Copie a versão LEGÍTIMA do HytaleServer.jar da instalação local (não distribuir no ZIP).
    compileOnly(files("libs/HytaleServer.jar"))
}
tasks.jar { archiveBaseName.set("LutiRPG") }
tasks.register<JavaExec>("smokeTest") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.com.lutiplay.rpg.CoreSmokeTest")
}
tasks.check { dependsOn("smokeTest") }
tasks.register<JavaExec>("regressionTest") {
    dependsOn(tasks.classes)
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("br.com.lutiplay.rpg.CoreRegressionTest")
}
tasks.check { dependsOn("regressionTest") }

