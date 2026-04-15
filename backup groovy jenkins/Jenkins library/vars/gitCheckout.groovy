// vars/gitCheckout.groovy
def call(Map stageParams = [:]) {
    def branch = stageParams.branch
    def repoUrl = stageParams.url

    if (!branch || !repoUrl) {
        error("[gitCheckout] Missing required parameters: 'branch' and 'url'")
    }

    echo "[gitCheckout] Checking out branch '${branch}' from '${repoUrl}'"

    checkout([
        $class: 'GitSCM',
        branches: [[name: branch]],
        userRemoteConfigs: [[url: repoUrl]]
    ])
}
