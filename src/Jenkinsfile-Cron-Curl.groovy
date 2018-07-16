import groovy.json.JsonSlurper
def initURI(){
    def jsonSlurper = new JsonSlurper()
    def filePath = "/var/jenkins_home/jobs/${env.JOB_NAME}/configure.json"
    //def filePath = "configure.json"
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$filePath"),"UTF-8"))
    def configuration = jsonSlurper.parse(reader)
    assert configuration instanceof Map
    def uri = "$configuration.method $configuration.url"
    return  uri
}

def getStatus(respondStr){
    def jsonSlurper = new JsonSlurper()
    def respondJson = jsonSlurper.parseText(respondStr)
    assert respondJson instanceof Map
    if(respondJson.status=="F"){
        error('Failing build because...')
        return false
    }
    return true
}

def getMessage(respondStr){
    def jsonSlurper = new JsonSlurper()
    def respondJson = jsonSlurper.parseText(respondStr)
    assert respondJson instanceof Map
    if(respondJson.status=="F"){
        return respondJson.message
    }
    return respondJson.message
}
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
