def call(Map params = [:]) {
    // Extract parameters
    def credentialsId        = params.get('credentialsId')
    def sonarHostUrl         = params.get('sonarHostUrl', 'http://192.168.1.141:9000')  // Default to local SonarQube if not provided
    def sonarProjectKey      = params.get('sonarProjectKey')
    def sonarProjectName     = params.get('sonarProjectName')
    def sonarProjectVersion  = params.get('sonarProjectVersion')

    // Ensure credentialsId is provided
    if (!credentialsId) {
        error "Missing required parameter: credentialsId"
    }

    // Use the SonarQube environment
    withSonarQubeEnv(credentialsId: credentialsId) {
        echo "Using SonarQube host: ${sonarHostUrl}"
        env.SONAR_HOST_URL = sonarHostUrl

        // Construct the SonarQube command to run analysis
        def sonarCmd = 'mvn clean package sonar:sonar'

        // Append project details if provided
        if (sonarProjectKey && sonarProjectName && sonarProjectVersion) {
            sonarCmd += " -Dsonar.projectKey='${sonarProjectKey}'"
            sonarCmd += " -Dsonar.projectName='${sonarProjectName}'"
            sonarCmd += " -Dsonar.projectVersion='${sonarProjectVersion}'"
        }

        // Run the SonarQube analysis command
        echo "Executing SonarQube analysis with command: ${sonarCmd}"
        sh sonarCmd
    }
}
