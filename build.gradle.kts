plugins {
    java
    `java-library`
    maven
}

val minestomVersion = "238ea649ab"
val cloudVersion = "1.4.0"

group = "io.github.openminigameserver"
//Use the same version as cloud
version = cloudVersion

repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven("https://libraries.minecraft.net")
    maven("https://repo.spongepowered.org/maven")
}
dependencies {
    api("cloud.commandframework:cloud-core:$cloudVersion")
    compileOnly("com.github.Minestom:Minestom:$minestomVersion")
}
