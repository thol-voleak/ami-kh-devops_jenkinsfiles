import groovy.json.JsonSlurper
def checkStatus(respondStr){
    def jsonSlurper = new JsonSlurper()
    def respondJson = jsonSlurper.parseText(respondStr)
    assert respondJson instanceof Map
    if(respondJson.status!="T"){
        env.FAILURE_STAGE = "StatusCode: " + respondJson.errorCode==null?respondJson.status:respondJson.errorCode + ", Message: " + respondJson.Message==null?respondJson.error:respondJson.Message
        error(respondJson.Message)
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
