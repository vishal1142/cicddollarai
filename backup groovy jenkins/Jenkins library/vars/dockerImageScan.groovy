def call(Map args) {
    def project   = args.ImageName ?: error("❌ Missing parameter: ImageName")
    def imageTag  = args.ImageTag ?: "latest"
    def hubUser   = args.DockerHubUser ?: error("❌ Missing parameter: DockerHubUser")

    // This variable is only available inside this method
    def fullImageName = "${hubUser}/${project}:${imageTag}"

    // Trivy Scan with DB preload
    sh """
        echo "📥 Preloading Trivy vulnerability database..."
        trivy image --download-db-only || true

        echo "🔍 Running Trivy scan on ${fullImageName}..."
        trivy image --timeout 5m ${fullImageName} --severity HIGH,CRITICAL --exit-code 1 --no-progress --quiet > scan.txt

        echo "📄 Vulnerability scan results:"
        cat scan.txt
    """
}
