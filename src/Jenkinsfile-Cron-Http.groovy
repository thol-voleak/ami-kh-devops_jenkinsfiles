import groovy.json.JsonSlurper
def invokerHttp(){
    def jsonSlurper = new JsonSlurper()
    def filePath = "/var/jenkins_home/jobs/${env.JOB_NAME}/configure.json"
    //def filePath = "configure.json"
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$filePath"),"UTF-8"))
    def configuration = jsonSlurper.parse(reader)
    assert configuration instanceof Map
    def post = null
    //try {
        post = new URL("$configuration.url").openConnection();
        post.setRequestMethod("$configuration.method")
        post.setConnectTimeout(30000)
        post.setReadTimeout(30000)
        post.setDoOutput(true)
        if ("$configuration.method" == "POST") {
            post.setRequestProperty("Content-Type", "application/json")
            def data = "$configuration.data"
            post.getOutputStream().write(data.getBytes("UTF-8"));
        }
        def postRC = post.getResponseCode();
        if (!postRC.equals(200)) {
            env.FAILURE_STAGE = "Error Code: " + post.getResponseCode() + ", Messages: Please click link ->"
            error("Error Code: " + post.getResponseCode())
        }
    //}catch (Exception e){
        e.printStackTrace()
        env.FAILURE_STAGE ="Connection request timeout"
        error("Connection request timeout")
    //}
    def respond = jsonSlurper.parseText(post.getInputStream().getText())
    assert respond instanceof Map
    if(respond.status=="F"){
        env.FAILURE_STAGE = "Error Code: " + respond.errorCode + ", Message: " + respond.onlyMessage
        error(respond.errorCode)
    }
}
pipeline {
    agent any
    stages{
        stage("Regression Test"){
            steps{
                build("Test-Jmeter")
            }
        }
        stage("Invoker") {
            steps{
                script{
                    invokerHttp()
                }
            }
        }
    }
    post {
        success{
            sh "echo sucess"
            slackSend (color: '#33ff36', message: "Sucessed built: Job '${env.JOB_NAME} [${env.BUILD_NUMBER} (<${env.BUILD_URL}|Detail>)]'")
        }
        failure {
            sh "echo failed"
            slackSend (color: '#FF0000', message: "Failed build:: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nReason: [${env.FAILURE_STAGE} (<${env.BUILD_URL}|Detail>)]'")
        }
    }
}
