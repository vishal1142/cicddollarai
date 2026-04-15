def call(Map config = [:]) {
    def imageName = config.ImageName ?: error("ImageName is required")
    def imageTag = config.ImageTag ?: "latest"

    def fullImageName = "${imageName}:${imageTag}"

    echo "Removing Docker image: ${fullImageName}"

    sh """
        docker rmi -f ${fullImageName} || echo 'Image not found or already removed.'
        docker image prune -f
    """
}
