import groovy.json.JsonSlurper
def __check_status(respond_str){
    def jsonSlurper = new JsonSlurper()
    def respond_json = jsonSlurper.parseText(respond_str)
    assert respond_json instanceof Map
    if(respond_json.status!="T"){
        def msg = respond_json.Message==null?respond_json.error:respond_json.Message
        def code = respond_json.errorCode==null?respond_json.status:respond_json.errorCode
        env.FAILURE_STAGE = "StatusCode: ${code} ,Message: ${msg}" 
        error("${code}")
    }
}
pipeline {
    agent any
    stages{
        stage("__call") {
            steps{
                script{   
                    def re = sh (script: "curl -X ${METHOD} ${CURL_URL}", returnStdout: true)
                    sh "echo repond: ${re}"
                    __check_status("${re}")
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
