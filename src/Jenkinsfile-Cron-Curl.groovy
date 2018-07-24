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
            sh "echo failed"
            slackSend (color: '#FF0000', message: "Failed build:: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nReason: [${env.FAILURE_STAGE} (<${env.BUILD_URL}|Detail>)]'")
        }
    }
}
