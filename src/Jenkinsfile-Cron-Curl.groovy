import groovy.json.JsonSlurper

pipeline {
    agent any
    stages{
        stage("Invoker") {
            steps{
                script{
                    def uri = initURI()
                    def re = sh (script: "curl -X ${uri}", returnStdout: true)
                    def status = getStatus("${re}")
                    def message = getMessage("${re}")
                }
            }
        }
    }
    post {
        success{
            //sh "echo ${message}"
            slackSend (color: '#33ff36', message: "Sucessed built: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nReason: (hehe)\nView Report: (${env.BUILD_URL})'")
        }
        failure {
            //sh "echo ${message}"
            slackSend (color: '#33ff36', message: "Failed build:: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\\nReason: (hehe)\nView Report: (${env.BUILD_URL})'")
        }
    }
}
