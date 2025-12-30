@Library("tanzeel-shared-library") _

def buildProfile = new org.mcafee.orbitbuild(this)  // ✅ Add 'def' and pass 'this'

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
