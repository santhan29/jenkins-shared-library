def call(Map configMap){
    pipeline { 
        agent {
            label 'AGENT-1' 
        }
        options{
            timeout(time: 30, unit: 'MINUTES') 
            disableConcurrentBuilds() 
        }
        environment{
            appVersion = ''  // we can use this env variables across the pipeline 
            region = 'us-east-1' 
            account_id = '361769595563'
            project = configMap.get("project")  
            environment = 'dev'
            component = configMap.get("component") 
        }
        stages {
            stage('Read the version') { 
                steps {
                    script{
                        def packageJson = readJSON file: 'package.json'
                        appVersion = packageJson.version
                        echo "App version: ${appVersion}" 
                    } 
                }
            }
            stage('Install Dependencies') {
                steps {
                    sh 'npm install'
                }
            }
        // stage('sonarqube analysis') {
        //     environment {
        //         SCANNER_HOME = tool 'sonar-6.0' //scanner configuration
        //     }
        //     steps {
        //         withSonarQubeEnv('sonar-6.0') {
        //             sh '$SCANNER_HOME/bin/sonar-scanner'
        //             //generic scanner, it automatically understands the language and provide scan results
        //         } 
        //     }
        // }
        // stage('SQuality Gate') {
        //     steps {
        //         timeout(time: 5, unit: 'MINUTES') {
        //             waitForQualityGate abortPipeline: true
        //         }
        //     }
        // } 
            stage('Docker build') {
                steps { 
                    withAWS(region: 'us-east-1', credentials: 'aws-credentials') {
                        sh """
                            aws ecr get-login-password --region ${region} | docker login --username AWS --password-stdin ${account_id}.dkr.ecr.us-east-1.amazonaws.com

                            docker build -t ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion} . 

                            docker images

                            docker push ${account_id}.dkr.ecr.us-east-1.amazonaws.com/${project}/${environment}/${component}:${appVersion}
                        """
                    } 
                }   
            }  
            stage('Deploy') {
                steps {
                    withAWS(region: 'us-east-1', credentials: 'aws-credentials') {
                        sh """
                            aws eks update-kubeconfig --region ${region} --name ${project}-${environment}

                            cd helm

                            sed -i 's/IMAGE_VERSION/${appVersion}/g' values-${environment}.yaml

                            helm upgrade --install ${component} -n ${project} -f values-${environment}.yaml . 
                        """

                    }
                }    
             } 
        } 
        post{
            always{
                echo "this section runs always" 
                deleteDir() 
            }
            success{
                echo "this section runs when pipeline is success"
            }
            failure{
                echo "this section runs when pipeline is failure" 
            }
        }
    } 
}