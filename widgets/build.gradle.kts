// SPDX-FileCopyrightText: 2026 Ashish Yadav <mailtoashish693@gmail.com>
// SPDX-License-Identifier: GPL-3.0-or-later
import com.android.build.api.dsl.LibraryExtension
import com.ichi2.anki.gradle.addAnkiBackendDependencies

plugins {
    id("ankidroid.android.library")
    id("ankidroid.android.compose")
}

configure<LibraryExtension> {
    namespace = "com.ichi2.anki.widgets"
}

dependencies {
    api(libs.androidx.glance.appwidget)
    implementation(libs.androidx.activity.compose)
    implementation(project(":anki-common"))
    implementation(project(":common"))
    implementation(project(":common:android"))
    implementation(project(":compat"))
    implementation(project(":libanki"))

    addAnkiBackendDependencies(project)

    implementation(libs.androidx.annotation)
    implementation(libs.androidx.core)
    implementation(libs.google.material)
    implementation(libs.jakewharton.timber)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(testFixtures(project(":anki-common")))
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.junit)
    testImplementation(libs.junit.platform.launcher)
    testImplementation(libs.junit.vintage.engine)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.mockk)
    testImplementation(libs.robolectric)
}
