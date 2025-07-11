plugins {
    java
    alias(libs.plugins.shadow)
}

group = "com.filizes"
version = "2.0"

java {
    sourceCompatibility = JavaVersion.VERSION_16
    targetCompatibility = JavaVersion.VERSION_16
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://repo.panda-lang.org/releases")
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.placeholderapi)

    compileOnly(libs.lombok)
    annotationProcessor(libs.lombok)

    implementation(libs.hikari)
    implementation(libs.guava)
    implementation(libs.okhttp)
    implementation(libs.sqlite)
    implementation(libs.guice)
    implementation(libs.caffeine)
    implementation(libs.litecommands)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit)

    testImplementation(platform(libs.mockito.bom))
    testImplementation(libs.mockito)

    testImplementation(libs.paper.api)
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    filteringCharset = "UTF-8"
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-parameters")
}

tasks.shadowJar {
    minimize()

    relocate("dev.rollczi.litecommands", "${project.group}.${project.name}.libs.litecommands")
    relocate("com.google.inject", "${project.group}.${project.name}.libs.guice")
    relocate("javax.inject", "${project.group}.${project.name}.libs.javaxinject")
    relocate("aopalliance", "${project.group}.${project.name}.libs.aopalliance")
    relocate("com.zaxxer.hikari", "${project.group}.${project.name}.libs.hikari")
    relocate("org.sqlite", "${project.group}.${project.name}.libs.sqlite")
    relocate("com.github.benmanes.caffeine", "${project.group}.${project.name}.libs.caffeine")
    relocate("okhttp3", "${project.group}.${project.name}.libs.okhttp3")
    relocate("okio", "${project.group}.${project.name}.libs.okio")

    exclude("META-INF/*.SF")
    exclude("META-INF/*.DSA")
    exclude("META-INF/*.RSA")
    exclude("META-INF/LICENSE*")
    exclude("META-INF/NOTICE*")
    exclude("**/module-info.class")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

