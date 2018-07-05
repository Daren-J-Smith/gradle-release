package net.researchgate.release.task

import net.researchgate.release.ReleaseExtension
import org.apache.tools.ant.BuildException
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class UnSnapshotVersionTask extends ReleaseBaseTask {

    @TaskAction
    void unSnapshotVersion() {
        extension = project.getExtensions().getByName('release')
        File propertiesFile = project.file(extension.versionPropertyFile)
        if (!propertiesFile.file) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion('1.0.0')
            }

            if (!useAutomaticVersion() && promptYesOrNo('Do you want to use SNAPSHOT versions inbetween releases')) {
                attributes.usesSnapshot = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, 'version', project.version)
                attributes.propertiesFileCreated = true
            } else {
                logger.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
                throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
            }
        }

        if (!propertiesFile.canRead() || !propertiesFile.canWrite()) {
            throw new GradleException("Unable to update version property. Please check file permissions.")
        }

        Properties properties = new Properties()
        propertiesFile.withReader { properties.load(it) }

        assert properties.version, "[$propertiesFile.canonicalPath] contains no 'version' property"
        assert extension.versionPatterns.keySet().any { (properties.version =~ it).find() },
                "[$propertiesFile.canonicalPath] version [$properties.version] doesn't match any of known version patterns: " +
                        extension.versionPatterns.keySet()

        // set the project version from the properties file if it was not otherwise specified
        if (!isVersionDefined()) {
            project.version = properties.version
        }

        def version = project.version.toString()

        if (version.contains('-SNAPSHOT')) {
            attributes.usesSnapshot = true
            version -= '-SNAPSHOT'
            updateVersionProperty(version)
        }
    }

    boolean isVersionDefined() {
        project.version && Project.DEFAULT_VERSION != project.version
    }

    String getReleaseVersion(String candidateVersion = "${project.version}") {
        String releaseVersion = findProperty('release.releaseVersion', null, 'releaseVersion')

        if (useAutomaticVersion()) {
            return releaseVersion ?: candidateVersion
        }

        return readLine("This release version:", releaseVersion ?: candidateVersion)
    }

    boolean useAutomaticVersion() {
        findProperty('release.useAutomaticVersion', null, 'gradle.release.useAutomaticVersion') == 'true'
    }

    String findProperty(String key, Object defaultVal = null, String deprecatedKey = null) {
        def property = System.getProperty(key) ?: project.hasProperty(key) ? project.property(key) : null

        if (!property && deprecatedKey) {
            property = System.getProperty(deprecatedKey) ?: project.hasProperty(deprecatedKey) ? project.property(deprecatedKey) : null
            if (property) {
                logger.warn("You are using the deprecated parameter '${deprecatedKey}'. Please use the new parameter '$key'. The deprecated parameter will be removed in 3.0")
            }
        }

        property ?: defaultVal
    }

    File findPropertiesFile() {
        File propertiesFile = project.file(extension.versionPropertyFile)
        if (!propertiesFile.file) {
            if (!isVersionDefined()) {
                project.version = getReleaseVersion('1.0.0')
            }

            if (!useAutomaticVersion() && promptYesOrNo('Do you want to use SNAPSHOT versions inbetween releases')) {
                attributes.usesSnapshot = true
            }

            if (useAutomaticVersion() || promptYesOrNo("[$propertiesFile.canonicalPath] not found, create it with version = ${project.version}")) {
                writeVersion(propertiesFile, 'version', project.version)
                attributes.propertiesFileCreated = true
            } else {
                logger.debug "[$propertiesFile.canonicalPath] was not found, and user opted out of it being created. Throwing exception."
                throw new GradleException("[$propertiesFile.canonicalPath] not found and you opted out of it being created,\n please create it manually and specify the version property.")
            }
        }
        propertiesFile
    }

    /**
     * Updates properties file (<code>gradle.properties</code> by default) with new version specified.
     * If configured in plugin convention then updates other properties in file additionally to <code>version</code> property
     *
     * @param newVersion new version to store in the file
     */
    void updateVersionProperty(String newVersion) {
        String oldVersion = project.version as String
        if (oldVersion != newVersion) {
            project.version = newVersion
            attributes.versionModified = true
            project.subprojects?.each { it.version = newVersion }
            List<String> versionProperties = extension.versionProperties + 'version'
            versionProperties.each { writeVersion(findPropertiesFile(), it, project.version) }
        }
    }

    protected void writeVersion(File file, String key, version) {
        try {
            if (!file.file) {
                project.ant.echo(file: file, message: "$key=$version")
            } else {
                // we use replace here as other ant tasks escape and modify the whole file
                project.ant.replaceregexp(file: file, byline: true) {
                    regexp(pattern: "^(\\s*)$key((\\s*[=|:]\\s*)|(\\s+)).+\$")
                    substitution(expression: "\\1$key\\2$version")
                }
            }
        } catch (BuildException be) {
            throw new GradleException('Unable to write version property.', be)
        }
    }

    /**
     * Reads user input from the console.
     *
     * @param message Message to display
     * @param defaultValue (optional) default value to display
     * @return User input entered or default value if user enters no data
     */
    protected static String readLine(String message, String defaultValue = null) {
        String _message = "$PROMPT $message" + (defaultValue ? " [$defaultValue] " : "")
        if (System.console()) {
            return System.console().readLine(_message) ?: defaultValue
        }
        println "$_message (WAITING FOR INPUT BELOW)"

        System.in.newReader().readLine() ?: defaultValue
    }

    private static boolean promptYesOrNo(String message, boolean defaultValue = false) {
        String defaultStr = defaultValue ? 'Y' : 'n'
        String consoleVal = readLine("${message} (Y|n)", defaultStr)
        if (consoleVal) {
            return consoleVal.toLowerCase().startsWith('y')
        }

        defaultValue
    }
}
