import groovy.json.JsonSlurper
def checkStatus(respondStr){
    def jsonSlurper = new JsonSlurper()
    def respondJson = jsonSlurper.parseText(respondStr)
    assert respondJson instanceof Map
    if(respondJson.status=="F"){
        env.FAILURE_STAGE = "Error Code: " + respond.errorCode + ", Message: " + respond.onlyMessage
        error(respond.errorCode)
    }
}
pipeline {
    agent any
    stages{
        stage("Invoker") {
            steps{
                script{
                    
                    def re = sh (script: "curl -X ${METHOD} ${CURL_URL}", returnStdout: true)
                    sh "echo zzzzzz ${re}"
                    checkStatus("${re}")
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
