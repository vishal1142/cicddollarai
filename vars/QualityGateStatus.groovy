def call(Map params = [:]) {
    def credentialsId = params.get('credentialsId')

    if (!credentialsId?.trim()) {
        error "[QualityGateStatus] Missing required parameter: 'credentialsId'"
    }

    echo "[QualityGateStatus] Waiting for SonarQube Quality Gate result..."
    echo "[QualityGateStatus] Using credentialsId: ${credentialsId}"

    waitForQualityGate(
        abortPipeline: true,
        credentialsId: credentialsId
    )
}
