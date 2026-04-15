// File: vars/DockerHelper.groovy

package org.mytools

class DockerHelper implements Serializable {
    def steps

    DockerHelper(steps) {
        this.steps = steps
    }

    // Define the performDockerOperations method
    void performDockerOperations(String project, String imageTag, String hubUser, String credId) {
        // Ensure Docker login before building the image
        this.steps.withCredentials([this.steps.usernamePassword(credentialsId: credId, usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASSWORD')]) {
            try {
                // Print the current working directory for debugging
                this.steps.sh 'pwd'
                this.steps.sh 'ls -la'

                // Login to DockerHub
                this.steps.sh """
                    echo "Logging in to DockerHub..."
                    echo "\$DOCKER_PASSWORD" | docker login -u \$DOCKER_USER --password-stdin
                """

                // Build Docker image
                this.steps.echo "Building Docker image ${hubUser}/${project}:${imageTag}..."
                this.steps.sh """
                    docker image build -t ${hubUser}/${project}:${imageTag} .
                    docker image tag ${hubUser}/${project}:${imageTag} ${hubUser}/${project}:latest
                    docker push ${hubUser}/${project}:${imageTag}
                    docker push ${hubUser}/${project}:latest
                """
            } catch (Exception e) {
                // Handle any error and provide better error message
                this.steps.echo "Docker build failed: ${e.message}"
                throw e
            }
        }
    }
}
