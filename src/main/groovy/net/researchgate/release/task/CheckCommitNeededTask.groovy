package net.researchgate.release.task

import org.gradle.api.tasks.TaskAction

class CheckCommitNeededTask extends ReleaseBaseTask {

    @TaskAction
    void checkCommitNeeded() {
        releasePlugin.scmAdapter.checkCommitNeeded()
    }
}
