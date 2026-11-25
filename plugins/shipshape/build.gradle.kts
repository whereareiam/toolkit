plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    `maven-publish`
}

dependencies {
    api(project(":plugins:shipshape:shipshape-api"))

    implementation(project(":plugins:shipshape:shipshape-common"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-buildfile"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-documentation"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-flow"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-format"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-imports"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-language"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-lombok"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-models"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-packages"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-size"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-tests"))
    implementation(project(":plugins:shipshape:shipshape-rule:rule-types"))

    testImplementation(gradleTestKit())
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")
}

gradlePlugin {
    plugins {
        create("shipshape") {
            id = "me.whereareiam.toolkit.shipshape"
            implementationClass = "me.whereareiam.toolkit.shipshape.ToolkitShipshapePlugin"
        }
    }
}
