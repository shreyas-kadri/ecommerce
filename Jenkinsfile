pipeline {
    agent any

    environment {
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
    }

    stages {
        stage('Checkout') {
            steps {
                echo 'Cloning the repository...'
                checkout scm
            }
        }

        stage('Build & Test Microservices') {
            parallel {
                stage('UserService') {
                    steps {
                        dir('UserService') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('ProductService') {
                    steps {
                        dir('ProductService') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('CartService') {
                    steps {
                        dir('CartService') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('InventoryService') {
                    steps {
                        dir('InventoryService') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('NotificationService') {
                    steps {
                        dir('NotificationService') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('APIGateway') {
                    steps {
                        dir('APIGateway') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }

                stage('EurekaServer') {
                    steps {
                        dir('EurekaServer') {
                            sh 'mvn clean compile'
                            sh 'mvn test'
                        }
                    }
                }
            }
        }
    }

    post {
        always {
            echo 'Build & Test Pipeline Completed.'
        }
        success {
            echo 'All services built and tested successfully!'
        }
        failure {
            echo 'One or more services failed to build/test.'
        }
    }
}
