import com.android.build.api.artifact.SingleArtifact
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Document
import org.w3c.dom.Element
import org.w3c.dom.NodeList

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

// The emulator reaches the PC's localhost at 10.0.2.2. Override with -PapiBaseUrl=... (it must end with "/").
val apiBaseUrl = providers.gradleProperty("apiBaseUrl").getOrElse("http://10.0.2.2:8080/")

android {
    namespace = "es.ivanstg.bandrepertoire"
    // Current AndroidX/Compose releases require compiling against API 37; targetSdk stays at 36.
    compileSdk = 37
    buildToolsVersion = "37.0.0"

    defaultConfig {
        applicationId = "es.ivanstg.bandrepertoire"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    lint {
        // Security checks are raised to errors in lint.xml; any error fails the build. No baseline.
        abortOnError = true
        checkReleaseBuilds = true
        lintConfig = file("lint.xml")
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    debugImplementation(libs.compose.ui.tooling)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)
}

// ---------------------------------------------------------------------------------------------------------------
// Security checks S2, S3 and S5: fail the build if the release app could be debugged, back up its data, talk
// plain HTTP, ask for permissions beyond INTERNET, or expose components to other apps.
// ---------------------------------------------------------------------------------------------------------------

abstract class CheckReleaseManifestTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val mergedManifest: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val networkSecurityConfig: RegularFileProperty

    @get:Input
    abstract val applicationId: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun check() {
        val android = "http://schemas.android.com/apk/res/android"
        val appId = applicationId.get()
        val problems = mutableListOf<String>()

        val manifest = parseXml(mergedManifest.get().asFile)
        // INTERNET, plus the signature-level permission androidx.core adds to keep dynamic receivers private.
        val allowedPermissions = setOf("android.permission.INTERNET", "$appId.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
        manifest.getElementsByTagName("uses-permission").elements().forEach {
            val name = it.getAttributeNS(android, "name")
            if (name !in allowedPermissions) problems += "Unexpected permission: $name"
        }

        val application = manifest.getElementsByTagName("application").elements().single()
        fun attribute(name: String) = application.getAttributeNS(android, name)
        if (attribute("debuggable") == "true") problems += "The release build is debuggable"
        if (attribute("allowBackup") != "false") problems += "android:allowBackup must be false"
        if (attribute("dataExtractionRules").isEmpty()) problems += "android:dataExtractionRules is missing"
        if (attribute("usesCleartextTraffic") == "true") problems += "android:usesCleartextTraffic is true"
        if (attribute("networkSecurityConfig").isEmpty()) problems += "android:networkSecurityConfig is missing"

        // Only the launcher activity may be open to other apps, unless a component is guarded by a permission.
        listOf("activity", "activity-alias", "service", "receiver", "provider").forEach { tag ->
            manifest.getElementsByTagName(tag).elements().forEach {
                val name = it.getAttributeNS(android, "name")
                val exported = it.getAttributeNS(android, "exported") == "true"
                val guarded = it.getAttributeNS(android, "permission").isNotEmpty()
                if (exported && !guarded && name != "$appId.MainActivity") problems += "Exported without a permission: $name"
            }
        }

        val config = parseXml(networkSecurityConfig.get().asFile)
        val base = config.getElementsByTagName("base-config").elements().singleOrNull()
        if (base?.getAttribute("cleartextTrafficPermitted") != "false") {
            problems += "The release network security config must set cleartextTrafficPermitted=\"false\" on base-config"
        }
        config.getElementsByTagName("domain-config").elements().forEach {
            if (it.getAttribute("cleartextTrafficPermitted") == "true") problems += "The release network security config allows cleartext for a domain"
        }

        val reportFile = report.get().asFile
        reportFile.parentFile.mkdirs()
        reportFile.writeText(if (problems.isEmpty()) "OK\n" else problems.joinToString("\n", postfix = "\n"))
        if (problems.isNotEmpty()) {
            throw GradleException("Release security check failed:\n - " + problems.joinToString("\n - "))
        }
    }

    private fun parseXml(file: File): Document = DocumentBuilderFactory.newInstance()
        .apply {
            isNamespaceAware = true
            setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        }
        .newDocumentBuilder()
        .parse(file)

    private fun NodeList.elements(): List<Element> = (0 until length).map { item(it) as Element }
}

androidComponents {
    onVariants(selector().withBuildType("release")) { variant ->
        // Not "checkReleaseManifest": AGP 9 already registers a task with that name.
        tasks.register<CheckReleaseManifestTask>("verifyReleaseSecurity") {
            group = "verification"
            description = "Fails if the release manifest or network security config breaks the security checklist."
            mergedManifest.set(variant.artifacts.get(SingleArtifact.MERGED_MANIFEST))
            // There is no release source set, so release builds use the main config (the debug one allows 10.0.2.2).
            networkSecurityConfig.set(layout.projectDirectory.file("src/main/res/xml/network_security_config.xml"))
            applicationId.set(variant.applicationId)
            report.set(layout.buildDirectory.file("reports/security/release-manifest.txt"))
        }
    }
}

tasks.named("check") { dependsOn("verifyReleaseSecurity") }
