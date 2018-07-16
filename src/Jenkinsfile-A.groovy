pipeline {
  agent any
    tools {
        maven 'MAVEN_HOME'
    }
    stages{
        stage('Checkout SCM'){
          steps{
            script{
              git branch: '$BRANCH',credentialsId: 'devops-bitbucket-credential',url: '$GIT_URL'
            }
          }
        }
        stage('Build Code'){
            steps{
              sh "mvn clean install -DskipTests"
            }
        }
        stage('Smoke Test+Push'){
            steps{
              sh "$OC login -u$OCP_USER_NAME -p$OCP_PWD --server=$OCP_SERVER --certificate-authority=$OCP_CERT_PATH"
              script{
                try{
                  sh "$OC get project ${PRO_NAME}"
                }catch(Exception ex) {
                  sh "$OC new-project ${PRO_NAME}"
                }
              }
              sh "mvn clean install docker:build docker:push"
            }
        }
        
        stage('Deployment'){ 
            steps{ 
              script{
                sh "$OC project ${PRO_NAME}"
                try{
                  sh "$OC get svc service-a"
                  sh '$OC set route-backends ${IMG_NAME} service-a=0 service-b=100'
                  sh "$OC rollout latest dc/service-a -n ${PRO_NAME}"
                  sh "$OC rollout status dc/service-a"
                  sh '$OC set route-backends ${IMG_NAME} service-a=100 service-b=0'
                }catch(Exception ex){
                  sh "$OC new-app ${PRO_NAME}/${IMG_NAME}:latest --name=service-a"
                  sh "$OC expose svc/service-a --hostname=service-a-${IMG_NAME}.apps.$OCP_BASE_URL"
                  sh "$OC expose svc/service-a --hostname=${IMG_NAME}.apps.$OCP_BASE_URL"
                }
              }
            }
        }
    }
    post { 
        success{ 
            sh '$REDIS_CLI -h $REDIS_HOST SET is-require-${PRO_NAME}-b-update Y'
            slackSend (color: '#33ff36', message: "Sucessed built: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nView Report: (${env.BUILD_URL})'")
        }
        failure {
          sh "$OC rollout cancel dc/service-a"
          //slackSend (color: '#ff9f33', message: "Failed build: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nView Report: (${env.BUILD_URL})'")
        }
    }
}
