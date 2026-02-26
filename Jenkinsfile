pipeline {
    agent any

    environment {
        AWS_REGION   = "us-east-1"
        ACCOUNT_ID   = "975050049140"
        CLUSTER_NAME = "eksdemo-cluster"
        ECR_REPO     = "eksdemo-repo"
        IMAGE_TAG    = "${BUILD_NUMBER}"
        IMAGE_URI    = "${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com/${ECR_REPO}:${IMAGE_TAG}"
        HOME         = "/var/lib/jenkins"
        KUBECONFIG   = "/var/lib/jenkins/.kube/config"
    }

    stages {

        stage('Checkout App Code') {
            steps {
                checkout scm
            }
        }

        stage('Checkout Infra Repo') {
            steps {
                dir('infra') {
                    git branch: 'main',
                        url: 'https://github.com/Writamgd/eks-terraform-infra.git'
                }
            }
        }

        stage('Terraform Init & Apply') {
            steps {
                dir('infra/environments/dev') {
                    sh 'terraform init'
                    sh 'terraform apply -auto-approve'
                }
            }
        }

        stage('Build & Run Unit Tests') {
            steps {
                sh 'mvn clean package -DskipTests'
            }
        }

        stage('Build Docker Image') {
            steps {
                sh "docker build -t ${ECR_REPO}:${IMAGE_TAG} ."
            }
        }

        stage('Login to ECR') {
            steps {
                sh """
                aws ecr get-login-password --region ${AWS_REGION} \
                | docker login --username AWS --password-stdin \
                ${ACCOUNT_ID}.dkr.ecr.${AWS_REGION}.amazonaws.com
                """
            }
        }

        stage('Create ECR Repository If Not Exists') {
            steps {
                sh """
                aws ecr describe-repositories --repository-names ${ECR_REPO} --region ${AWS_REGION} \
                || aws ecr create-repository --repository-name ${ECR_REPO} --region ${AWS_REGION}
                """
            }
        }

        stage('Tag & Push Image') {
            steps {
                sh """
                docker tag ${ECR_REPO}:${IMAGE_TAG} ${IMAGE_URI}
                docker push ${IMAGE_URI}
                """
            }
        }

        stage('Update kubeconfig') {
            steps {
                sh """
                mkdir -p $HOME/.kube
                aws eks update-kubeconfig \
                --region ${AWS_REGION} \
                --name ${CLUSTER_NAME} \
                --kubeconfig ${KUBECONFIG}
                """
            }
        }

        stage('Deploy to EKS') {
            steps {
                sh """
                # Apply ALL manifests (deployment + service)
                kubectl apply -f k8s/

                # Update image to new build
                kubectl set image deployment/eksdemo-deployment \
                eksdemo-app=${IMAGE_URI}
                """
            }
        }

        stage('Verify Deployment') {
            steps {
                sh """
                kubectl rollout status deployment/eksdemo-deployment

                echo "Pods:"
                kubectl get pods

                echo "Services:"
                kubectl get svc
                """
            }
        }

        stage('Get LoadBalancer DNS') {
            steps {
                sh """
                echo "Waiting for LoadBalancer..."
                sleep 30

                DNS=$(kubectl get svc eksdemo-service \
                -o jsonpath='{.status.loadBalancer.ingress[0].hostname}')

                echo "======================================="
                echo "Application URL:"
                echo "http://$DNS"
                echo "======================================="
                """
            }
        }
    }

    post {
        success {
            echo "🚀 Deployment Successful! Image: ${IMAGE_URI}"
        }
        failure {
            echo "❌ Pipeline Failed! Check logs."
        }
    }
}