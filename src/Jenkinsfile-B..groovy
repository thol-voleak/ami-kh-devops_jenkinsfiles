pipeline {
    agent any
    stages{
        stage("Deployment") {
            steps{
                script{
                    def isupdate = sh (script: '$REDIS_CLI -h $REDIS_HOST GET is-require-${PRO_NAME}-b-update', returnStdout: true).trim()
                    def isinprocessing = sh (script: '$REDIS_CLI -h $REDIS_HOST GET is-${PRO_NAME}-inprocessing', returnStdout: true).trim()
                    if(isupdate.equals("Y") && isinprocessing.equals("N")){
                        sh '$OC login -u$OCP_USER_NAME -p$OCP_PWD --server=$OCP_SERVER --certificate-authority=$OCP_CERT_PATH'
                        sh '$OC project ${PRO_NAME}'
                        sh '$OC set route-backends ${IMG_NAME} service-a=100 service-b=0'
                        sh '$OC rollout latest dc/service-b -n ${PRO_NAME}'
                        sh '$OC rollout status dc/service-b'
                        sh '$OC set route-backends ${IMG_NAME} service-a=50 service-b=50'
                    }
                }
            }
        }
    }
    post { 
        success{ 
            script{
                sh '$REDIS_CLI -h $REDIS_HOST SET is-require-${PRO_NAME}-b-update N'
                sh '$REDIS_CLI -h $REDIS_HOST SET is-${PRO_NAME}-inprocessing N'
                slackSend (color: '#33ff36', message: "Sucessed built: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nView Report: (${env.BUILD_URL})'")
                
            }
        }
        failure {
          sh "$OC rollout cancel dc/service-b"
           sh '$REDIS_CLI -h $REDIS_HOST SET is-${PRO_NAME}-inprocessing N'
          slackSend (color: '#33ff36', message: "Failed build:: Job '${env.JOB_NAME} [${env.BUILD_NUMBER}]\nView Report: (${env.BUILD_URL})'")
        }
    }
}
