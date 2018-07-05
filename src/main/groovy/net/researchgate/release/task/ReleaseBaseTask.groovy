package net.researchgate.release.task

import net.researchgate.release.ReleaseExtension
import net.researchgate.release.ReleasePlugin
import net.researchgate.release.cli.Executor
import org.gradle.api.DefaultTask

class ReleaseBaseTask extends DefaultTask {

    protected static final String LINE_SEP = System.getProperty('line.separator')
    protected static final String PROMPT = "${LINE_SEP}??>"

    protected static Map<String, Object> attributes = [:]

    protected ReleaseExtension extension

    protected Executor executor

    protected ReleasePlugin releasePlugin

    void setReleasePlugin(ReleasePlugin releasePlugin) {
        this.releasePlugin = releasePlugin
    }

    ReleasePlugin getReleasePlugin() {
        this.releasePlugin
    }

}
