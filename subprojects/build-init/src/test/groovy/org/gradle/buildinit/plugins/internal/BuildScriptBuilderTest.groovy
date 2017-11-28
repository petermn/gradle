/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.gradle.buildinit.plugins.internal

import org.gradle.buildinit.plugins.fixtures.ScriptDslFixture
import org.gradle.test.fixtures.file.TestNameTestDirectoryProvider
import org.junit.Rule
import spock.lang.Specification
import spock.lang.Unroll

import static org.gradle.buildinit.plugins.internal.modifiers.BuildInitBuildScriptDsl.GROOVY
import static org.gradle.buildinit.plugins.internal.modifiers.BuildInitBuildScriptDsl.KOTLIN
import static org.gradle.util.TextUtil.toPlatformLineSeparators
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo

class BuildScriptBuilderTest extends Specification {

    @Rule
    TestNameTestDirectoryProvider tmpDir = new TestNameTestDirectoryProvider()
    def outputFile = tmpDir.file("build.gradle")
    def builder = new BuildScriptBuilder()

    @Unroll
    def "generates basic #scriptDsl build script"() {
        when:
        builder.create(scriptDsl, outputFile).generate()

        then:
        assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

""")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "can add #scriptDsl build script comment"() {
        when:
        builder.fileComment("""This is a sample
see more at gradle.org""")
        builder.create(scriptDsl, outputFile).generate()

        then:
        assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 *
 * This is a sample
 * see more at gradle.org
 */

""")

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "can add plugins to #scriptDsl build script"() {
        when:
        builder.plugin("Add support for the Java language", "java")
        builder.plugin("Add support for Java libraries", "java-library")
        builder.create(scriptDsl, outputFile).generate()

        then:
        if (GROOVY == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

plugins {
    // Add support for the Java language
    id 'java'

    // Add support for Java libraries
    id 'java-library'
}

""")
        }
        if (KOTLIN == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

plugins {
    // Add support for the Java language
    java

    // Add support for Java libraries
    `java-library`
}

""")
        }

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "can add compile dependencies to #scriptDsl build scripts"() {
        when:
        builder.compileDependency("Use slf4j", "org.slf4j:slf4j-api:2.7", "org.slf4j:slf4j-simple:2.7")
        builder.compileDependency("Use Scala to compile", "org.scala-lang:scala-library:2.10")
        builder.create(scriptDsl, outputFile).generate()

        then:
        if (GROOVY == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Use slf4j
    compile 'org.slf4j:slf4j-api:2.7'
    compile 'org.slf4j:slf4j-simple:2.7'

    // Use Scala to compile
    compile 'org.scala-lang:scala-library:2.10'
}

""")
        }
        if (KOTLIN == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // Use slf4j
    compile("org.slf4j:slf4j-api:2.7")
    compile("org.slf4j:slf4j-simple:2.7")

    // Use Scala to compile
    compile("org.scala-lang:scala-library:2.10")
}

""")
        }

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    @Unroll
    def "can add test compile and runtime dependencies to #scriptDsl build scripts"() {
        when:
        builder.testCompileDependency("use some test kit", "org:test:1.2", "org:test-utils:1.2")
        builder.testRuntimeDependency("needs some libraries at runtime", "org:test-runtime:1.2")
        builder.create(scriptDsl, outputFile).generate()

        then:
        if (GROOVY == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // use some test kit
    testCompile 'org:test:1.2'
    testCompile 'org:test-utils:1.2'

    // needs some libraries at runtime
    testRuntime 'org:test-runtime:1.2'
}

""")
        }
        if (KOTLIN == scriptDsl) {
            assertOutputFile("""/*
 * This build file was generated by the Gradle 'init' task.
 */

// In this section you declare where to find the dependencies of your project
repositories {
    // Use jcenter for resolving your dependencies.
    // You can declare any Maven/Ivy/file repository here.
    jcenter()
}

dependencies {
    // use some test kit
    testCompile("org:test:1.2")
    testCompile("org:test-utils:1.2")

    // needs some libraries at runtime
    testRuntime("org:test-runtime:1.2")
}

""")
        }

        where:
        scriptDsl << ScriptDslFixture.SCRIPT_DSLS
    }

    def "can add further configuration"() {
        given:
        builder
            .taskPropertyAssignment(null, "test", "Test", "maxParallelForks", 23)
            .conventionPropertyAssignment("Convention configuration A", "application", "mainClassName", "com.example.Main")
            .conventionPropertyAssignment("Convention configuration B", "application", "applicationName", "My Application")
            .taskMethodInvocation("Use TestNG", "test", "Test", "useTestNG")
            .taskPropertyAssignment("Disable tests", "test", "Test", "enabled", false)

        when:
        builder.create(GROOVY, outputFile).generate()

        then:
        assertOutputFileContains("""
            test {
                maxParallelForks = 23
            }

            // Convention configuration A
            mainClassName = 'com.example.Main'

            // Convention configuration B
            applicationName = 'My Application'

            test {
                // Use TestNG
                useTestNG()
            }

            test {
                // Disable tests
                enabled = false
            }
        """)

        when:
        builder.create(KOTLIN, outputFile).generate()

        then:
        assertOutputFileContains("""
            val test by tasks.getting(Test::class) {
                maxParallelForks = 23
            }

            application {
                // Convention configuration A
                mainClassName = "com.example.Main"
            }

            application {
                // Convention configuration B
                applicationName = "My Application"
            }
            
            val test by tasks.getting(Test::class) {
                // Use TestNG
                useTestNG()
            }

            val test by tasks.getting(Test::class) {
                // Disable tests
                isEnabled = false
            }
        """)
    }

    def assertOutputFileContains(String substring) {
        assert outputFile.file
        outputFile.assertContents(
            containsString(toPlatformLineSeparators(substring.stripIndent().trim())))
    }

    def assertOutputFile(String contents) {
        assert outputFile.file
        outputFile.assertContents(equalTo(toPlatformLineSeparators(contents)))
    }

}
