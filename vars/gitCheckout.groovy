// vars/gitCheckout.groovy
def call(Map stageParams = [:]) {
    def branch = stageParams.branch
    def repoUrl = stageParams.url

    if (!branch || !repoUrl) {
        error("[gitCheckout] Missing required parameters: 'branch' and 'url'")
    }

    def credId = stageParams.credentialsId ?: ''

    echo "[gitCheckout] Checking out branch '${branch}' from '${repoUrl}'"

    def remoteConfig = [url: repoUrl]
    if (credId?.trim()) {
        remoteConfig.credentialsId = credId.trim()
    }

    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [remoteConfig]
    ])
}
