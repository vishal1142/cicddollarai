def call(Map args) {
    def project = args.ImageName ?: error("Missing parameter: ImageName")
    def imageTag = args.ImageTag ?: "latest"
    def hubUser = args.DockerHubUser ?: error("Missing parameter: DockerHubUser")

    echo "Building Docker image: ${hubUser}/${project}:${imageTag}"

    try {
        sh """
            docker image build -t ${hubUser}/${project}:${imageTag} .
            docker image tag ${hubUser}/${project}:${imageTag} ${hubUser}/${project}:latest
        """
    } catch (Exception e) {
        error "Docker build or tag failed: ${e.message}"
    }
}
