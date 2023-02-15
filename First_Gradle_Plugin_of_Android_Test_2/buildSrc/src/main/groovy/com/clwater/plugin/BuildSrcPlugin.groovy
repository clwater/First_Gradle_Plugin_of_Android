package com.clwater.plugin
import org.gradle.TaskExecutionRequest
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.invocation.Gradle

class BuildSrcPlugin implements Plugin<Project>{

    @Override
    void apply(Project project) {
        println('==========================')
        println('BuildSrcPlugin is applying')
        println('==========================')

    }

}