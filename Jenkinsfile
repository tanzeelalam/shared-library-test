@Library("tanzeel-shared-library") _

def buildProfile = new org.mcafee.orbitbuild()  // ✅ Add 'def' and pass 'this'

pipeline {
    agent any
    stages {
        stage('Hello') {
            steps {
                echo 'Hello World'
                helloWorld()
            }
        }
    }
}
