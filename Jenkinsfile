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

        stage('Start Setup Dependencies') {
            steps {
                echo 'Running docker-compose in Setup folder to start dependencies...'
                dir('Setup') {
                    sh 'docker-compose up -d'
                }
                sh 'docker ps'
            }
        }

        stage('Build & Test Services') {
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

        stage('Start Final Application') {
            steps {
                echo 'Running docker-compose in project root to deploy all microservices...'
                sh 'docker-compose down || true' // ignore failure if not running
                sh 'docker-compose up -d'
                sh 'docker ps'
            }
        }
    }

    post {
        always {
            echo 'Build & Deploy pipeline completed.'
        }

        success {
            echo 'All services built, tested, and deployed successfully.'
        }

        failure {
            echo 'One or more services failed during the pipeline.'
        }
    }
}
