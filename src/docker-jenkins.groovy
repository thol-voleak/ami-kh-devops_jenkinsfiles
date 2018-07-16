import groovy.json.JsonSlurper
def __get_user(){
    def filePath = "/var/jenkins_home/configure.json"
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$filePath"),"UTF-8"))
    def jsonSlurper = new JsonSlurper()
    def config= jsonSlurper.parse(reader)
    assert config instanceof Map
    println(config.user)
    return  "$config.user"
}

def __get_token(){
    def filePath = "/var/jenkins_home/configure.json"
    def reader = new BufferedReader(new InputStreamReader(new FileInputStream("$filePath"),"UTF-8"))
    def jsonSlurper = new JsonSlurper()
    def config= jsonSlurper.parse(reader)
    assert config instanceof Map
    println(config.token)
    return  "$config.token"
}

pipeline {
    agent any
    tools {
        maven 'MAVEN_HOME'
    }
    stages{
        stage('Test') {
            steps {
                script {
                    sh "date"
                }
            }
        }
        stage('Checkout SCM'){
            steps{
                script{
                    git branch: 'master',credentialsId: 'devops-github-credential',url: 'https://github.com/thol-voleak/spring-boot-openshift-hello-world.git'
                }
            }
        }
        stage('Maven Clean'){
            steps{
                sh "mvn clean install -DskipTests"
            }
        }
        stage('Docker Build'){
            steps{
                sh 'docker build -t myapp:latest .'
            }
        }
        stage('Docker Push') {
            steps {
                script {
                    def user = __get_user()
                    def token = __get_token()
                    sh "echo user: ${user} , token: ${token}"
                    sh "docker login -u ${user} -p ${token} docker-registry-default.apps.master-ocp.truemoney.com.kh"
                    sh "docker tag myapp docker-registry-default.apps.master-ocp.truemoney.com.kh/ads/myapp:latest"
                    sh 'docker push docker-registry-default.apps.master-ocp.truemoney.com.kh/ads/myapp:latest'
                }
            }
        }
    }
}
