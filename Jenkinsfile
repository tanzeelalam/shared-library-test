@Library("tanzeel-shared-library") _

node {
    echo "Pipeline started successfully!"
    
    stage('Checkout') {
        echo 'Checking out code...'
    }
    
    stage('Build') {
        echo 'Building the application...'
    }
    
    stage('Test') {
        echo 'Running tests...'
    }
    
    stage('Deploy') {
        echo 'Deploying application...'
    }
    
    stage('Cleanup') {
        echo 'Cleaning up...'
        cleanWs()
    }
    
    echo "Pipeline completed successfully!"
}
