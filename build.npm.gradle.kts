tasks.register("buildNpm") {
    group = "build"
    description = "npm run build"
    doLast {
        val projectDir = buildscript.sourceFile?.parentFile

        project.exec {
            workingDir = projectDir
            commandLine("npm", "install")
        }

        project.exec {
            workingDir = projectDir
            commandLine("npm", "run", "build")
        }

        ant.withGroovyBuilder {
            "mkdir"("dir" to "${projectDir}/dist/")
            "move"("todir" to "${buildDir}/resources/main/static/", "overwrite" to true) {
                "fileset"("dir" to "${projectDir}/dist/")
            }
        }
    }
}
