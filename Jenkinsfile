//@Library("tanzeel-shared-library") _

//def buildProfile = new org.mcafee.orbitbuild()

node {
    stage('Checkout') {
        echo 'Checking out code...'
        // git 'https://github.com/your-repo.git'
    }
    
    stage('Build') {
        echo 'Building the application...'
        // sh 'mvn clean package'
    }
    
    stage('Test') {
        echo 'Running tests...'
        // sh 'mvn test'
    }
    
    stage('Deploy') {
        echo 'Deploying application...'
        // sh './deploy.sh'
    }
    
    stage('Cleanup') {
        echo 'Cleaning up...'
        cleanWs()
    }
}
