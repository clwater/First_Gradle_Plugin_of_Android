/*
 * This Groovy source file was generated by the Gradle 'init' task.
 */
package com.clwater.plugin

import org.gradle.api.Project
import org.gradle.api.Plugin

/**
 * A simple 'hello world' plugin.
 */
class PluginPlugin implements Plugin<Project> {
    void apply(Project project) {
        // Register a task
        project.tasks.register("greeting") {
            doLast {
                println("Hello from plugin 'com.clwater.plugin.greeting'")
            }
        }
    }
}
