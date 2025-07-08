// use an integer for version numbers
version = 1

dependencies {
    val apkTasks = listOf("deployWithAdb", "build")
    val useApk = gradle.startParameter.taskNames.any { taskName ->
        apkTasks.any { apkTask ->
            taskName.contains(apkTask, ignoreCase = true)
        }
    }

    val implementation by configurations
    val apk by configurations

    // If the task is specifically to compile the app then use the stubs, otherwise us the library.
    if (useApk) {
        // Stubs for all Cloudstream classes
        apk("com.lagradost:cloudstream3:pre-release")
    } else {
        // For running locally
        implementation("com.github.Blatzar:CloudstreamApi:0.1.6")
    }
    // https://mvnrepository.com/artifact/com.fasterxml.jackson.core/jackson-databind
    implementation("com.fasterxml.jackson.core:jackson-databind:2.19.1")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.19.1")
    // https://mvnrepository.com/artifact/junit/junit
    testImplementation("junit:junit:4.13.2")
// https://mvnrepository.com/artifact/org.jetbrains.kotlinx/kotlinx-coroutines-test
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.1")
    testImplementation(kotlin("test"))
}

cloudstream {
    language = "uk"
    // All of these properties are optional, you can safely remove them

    description = "hdrezka.co - Ми створили зручний та унікальний у своєму роді кінотеатр для перегляду відео в комфортних для тебе умовах.."
    authors = listOf("CakesTwix")

    /**
     * Status int as the following:
     * 0: Down
     * 1: Ok
     * 2: Slow
     * 3: Beta only
     * */
    status = 2 // will be 3 if unspecified
    iconUrl = "https://www.google.com/s2/favicons?domain=hdrezka.co&sz=%size%"
}
