@Library("tanzeel-shared-library") _

def buildProfile = new org.mcafee.orbitbuild()

pipeline {
    agent any
    stages {
        stage('Hello') {
            steps {
                script {
                    echo 'Hello World'
                    helloWorld()
                    
                    // Example: Use buildProfile methods
                    echo "Build Info: ${buildProfile.buildInfo}"
                    echo "Stash Number: ${buildProfile.stashNumber}"
                }
            }
        }
        
        stage('Use BuildProfile') {
            steps {
                script {
                    // Example: Add GIT repository
                    buildProfile.addGIT([
                        project: "/your-org/your-repo",
                        branch: "main",
                        server: "ssh://git@github.com",
                        instance: "github",
                        target: "/target-path"
                    ])
                    
                    // Example: Upload to Artifactory
                    buildProfile.uploadToArtifactory()
                    
                    // Example: Publish build info
                    buildProfile.publishBuildInfo()
                }
            }
        }
    }
}
