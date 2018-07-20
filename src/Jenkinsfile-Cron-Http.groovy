import groovy.json.JsonSlurper

def __call(){
    def jsonSlurper = new JsonSlurper()
    def filePath = "/var/jenkins_home/jobs/${env.JOB_NAME}/url-config.json"
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$filePath"),"UTF-8"))
    def configuration = jsonSlurper.parse(reader)
    assert configuration instanceof Map
    def post = null
    try {
        println("$configuration.url")
        post = new URL("$configuration.url").openConnection();
        post.setRequestMethod("$configuration.method")
        println(configuration.timeout)
        if(configuration.timeout == null){
            println("zzzzzzzzzzzzzzzzzzzzzz")
            configuration.timeout = 60000
        }
        println(configuration.timeout)
        post.setConnectTimeout(configuration.timeout)
        post.setReadTimeout(configuration.timeout)
        post.setDoOutput(true)
        if ("$configuration.method" == "POST") {
            post.setRequestProperty("Content-Type", "application/json")
            def data = "$configuration.data"
            post.getOutputStream().write(data.getBytes("UTF-8"));
        }
    }catch (SocketTimeoutException e){
        println(e.message)
        env.FAILURE_STAGE ="Error Code: SYS0001, Messages: Connection read timeout"
        error("Connection read timeout")
    }catch (Exception e){
        println(e.message)
        env.FAILURE_STAGE ="Error Code: SYS0001, Messages: Connection request timeout"
        error("Connection request timeout")
    }
    def postRC = post.getResponseCode();
    if (!postRC.equals(200)) {
        env.FAILURE_STAGE = "Error Code: " + post.getResponseCode() + ", Messages: Please click link ->"
        error("Error Code: " + post.getResponseCode())
    }
    def respond = null
    try {
        def respondStr = post.getInputStream().getText()
        respond = jsonSlurper.parseText(respondStr)
        assert respond instanceof Map
    }catch (Exception e){
        println(e.message)
        env.FAILURE_STAGE ="Error Code: SYS0002, Messages: Incorrect respond format"
        error("Incorrect respond format")
    }
    if (respond.status == "F") {
        env.FAILURE_STAGE = "Error Code: " + respond.errorCode + ", Message: " + respond.onlyMessage
        error(respond.errorCode)
    }
}
pipeline {
    agent any
    stages{
        stage("Call") {
            steps{
                script{
                    __call()
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
