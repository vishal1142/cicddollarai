def call(String project, String ImageTag, String hubUser) {
    // Login to DockerHub using provided credentials
    withCredentials([usernamePassword(
        credentialsId: 'vishal',  // Replace with the correct Jenkins credentials ID for DockerHub
        usernameVariable: 'USER',
        passwordVariable: 'PASS'
    )]) {
        sh """
            echo "Logging into DockerHub with user: \$USER"
            docker login -u "\$USER" -p "\$PASS"
        """
    }

    // Define the full image name and latest tag
    def fullImageName = "${hubUser}/${project}:${ImageTag}"
    def latestImageName = "${hubUser}/${project}:latest"

    // Push the specific version of the image
    echo "Pushing Docker image: ${fullImageName}"
    sh "docker push ${fullImageName}"

    // Tag the image as 'latest'
    echo "Tagging image as latest: ${latestImageName}"
    sh "docker tag ${fullImageName} ${latestImageName}"

    // Push the latest image to DockerHub
    echo "Pushing Docker image: ${latestImageName}"
    sh "docker push ${latestImageName}"
}
