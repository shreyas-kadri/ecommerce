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

        stage('Start Dependencies') {
            steps {
                echo 'Running docker-compose in Setup folder...'
                dir('Setup') {
                    sh 'docker-compose up -d'
                }
                sh 'docker ps'
            }
        }

        stage('Build & Test UserService') {
            steps {
                dir('UserService') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test ProductService') {
            steps {
                dir('ProductService') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test CartService') {
            steps {
                dir('CartService') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test InventoryService') {
            steps {
                dir('InventoryService') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test NotificationService') {
            steps {
                dir('NotificationService') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test APIGateway') {
            steps {
                dir('APIGateway') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }

        stage('Build & Test EurekaServer') {
            steps {
                dir('EurekaServer') {
                    sh 'mvn clean compile'
                    sh 'mvn test'
                }
            }
        }
    }

    post {
        always {
            echo 'Build & Test pipeline completed.'

            // Optional: Stop and clean up dependency containers
            dir('Setup') {
                sh 'docker-compose down'
            }
        }
        success {
            echo 'All microservices built and tested successfully.'
        }
        failure {
            echo 'One or more services failed during build/test.'
        }
    }
}
