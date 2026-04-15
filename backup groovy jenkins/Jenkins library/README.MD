git init
git remote add origin https://github.com/vishal1142/jenkinslibrary.git
git add .
git commit -m "Initial commit"
git push -u origin master

hi

def call(String project, String imageTag, String hubUser){

    sh """
        docker image build -t ${hubUser}:${project} .
        docker image tag ${hubUser}:${project} ${hubUser}:${project}:${ImageTag}
        docker image tag ${hubUser}:${project} ${hubUser}:${project}:latest
    """
}












// def call(Map args) {
//     // Use parameter names that match the Jenkinsfile
//     def project = args.ImageName ?: error("Missing parameter: ImageName")  // Corresponds to 'ImageName' in Jenkinsfile
//     def imageTag = args.ImageTag ?: "latest"  // Default to 'latest' if not provided
//     def hubUser = args.DockerHubUser ?: error("Missing parameter: DockerHubUser")  // Corresponds to 'DockerHubUser' in Jenkinsfile

//     // Docker image full name
//     def fullImageName = "${hubUser}/${project}:${imageTag}"

//     // Trivy scan commands
//     sh """
//         echo "Running Trivy scan on ${fullImageName}..."
//         trivy image ${fullImageName} --severity HIGH,CRITICAL --exit-code 1 --no-progress --quiet > scan.txt
//         cat scan.txt
//     """
// }

//         // trivy image ${fullImageName} --severity HIGH,CRITICAL --exit-code 1 --no-progress --quiet > scan.txt
//         // cat scan.txt
//         // if grep -q 'HIGH|CRITICAL' scan.txt; then
//         //     echo 'Vulnerabilities found in the image. Please check the scan report.'
//         //     exit 1
//         // else
//         //     echo 'No vulnerabilities found in the image.'
//         // fi