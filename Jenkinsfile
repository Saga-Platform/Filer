pipeline {
    agent {
        kubernetes {
            inheritFrom 'jenkins-agent'
            yaml '''
                spec:
                  containers:
                  - name: docker
                    image: docker.int.slongpre.com:34443/buildx-cli:latest
                    securityContext:
                      privileged: true
                    env:
                    - name: DOCKER_CLI_EXPERIMENTAL
                      value: enabled
                    command: ["sleep"]
                    args: ["999999999"]

                  - name: gradle
                    image: gradle:jdk11
                    command: ["sleep"]
                    args: ["999999999"]

                  - name: kubectl
                    image: lachlanevenson/k8s-kubectl:v1.22.2
                    volumeMounts:
                    - name: config-volume
                      mountPath: "/root/.kube/"
                    command: ["sleep"]
                    args: ["999999999"]

                  volumes:
                  - name: config-volume
                    configMap:
                      name: jenkins-kubectl
                      items:
                      - key: config.yml
                        path: config
                '''
        }
    }

    environment {
        REGISTRY_URL = "docker.int.slongpre.com:34443"
        IMAGE_NAME = "saga-filer"
    }

    options {
        ansiColor('xterm')
    }

    stages {
        stage('Unit tests') {
            steps {
                container("gradle") {
                    sh "gradle test --no-daemon"
                }
            }
        }
        stage('Setup') {
            when {
               branch 'master'
            }
            steps {
                container('docker') {
                    // Creating build nodes context
                    sh "docker context create node-arm64 --docker host=tcp://pi4.int.slongpre.com:2376"
                    sh "docker context create node-amd64 --docker host=tcp://kube.int.slongpre.com:2376"
                    // Creating multi-node buildx instance
                    sh "docker buildx create --use --name agent-build node-amd64"
                    sh "docker buildx create --append --name agent-build node-arm64"
                }
            }
        }
        stage('Build Docker images') {
            when {
               branch 'master'
            }
            steps {
                parallel(
                        arm: {
                            container('docker') {
                                sh "docker buildx build --push --platform linux/amd64 -t $REGISTRY_URL/$IMAGE_NAME:amd64 ."
                            }
                        },
                        amd: {
                            container('docker') {
                                sh "docker buildx build --push --platform linux/arm64 -t $REGISTRY_URL/$IMAGE_NAME:arm64 ."

                            }
                        }
                )
            }
        }
        stage('Create and publish manifests') {
            when {
               branch 'master'
            }
            steps {
                container('docker') {
                    sh 'docker manifest create $REGISTRY_URL/$IMAGE_NAME:$(date +%Y%m%d) $REGISTRY_URL/$IMAGE_NAME:amd64 $REGISTRY_URL/$IMAGE_NAME:arm64'
                    sh 'docker manifest push $REGISTRY_URL/$IMAGE_NAME:$(date +%Y%m%d)'
                    sh "docker manifest create $REGISTRY_URL/$IMAGE_NAME:latest $REGISTRY_URL/$IMAGE_NAME:amd64 $REGISTRY_URL/$IMAGE_NAME:arm64"
                    sh "docker manifest push $REGISTRY_URL/$IMAGE_NAME:latest"
                }
            }
        }
        stage('Deploy to Kubernetes') {
            when {
               branch 'master'
            }
            steps {
                container('kubectl') {
                    sh 'kubectl apply -f ./k8s.yaml'
                    sh 'kubectl rollout restart -n saga deployment filer'
                }
            }
        }
    }
    post {
        always {
            junit 'build/test-results/**/*.xml'
        }
    }
}