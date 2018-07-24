import groovy.json.JsonSlurper

pipeline {
    agent any
    stages{
        stage("Invoker") {
            steps{
                script{
                    sh "echo zzzzzz ${CURL_URL}"
                    def re = sh (script: "curl -X ${METHOD} ${CURL_URL}", returnStdout: true)
                    def status = getStatus("${re}")
                    def message = getMessage("${re}")
                }
            }
        }
    }
    post {
        failure {
            //sh "echo ${message}"
            slackSend (color: '#33ff36', message: "Failed build:: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\\nReason: (hehe)\nView Report: (${env.BUILD_URL})'")
        }
    }
}
