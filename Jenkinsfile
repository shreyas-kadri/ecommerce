pipeline {
    agent any

    tools {
        jdk 'jdk-17'
    }

    environment {
        MAVEN_OPTS = '-Dmaven.test.failure.ignore=false'
    }

    stages {
        stage('Start Setup Dependencies') {
            steps {
                echo 'Running docker-compose in Setup folder to start dependencies...'
                dir('Setup') {
                    bat 'docker-compose up -d'
                }
                bat 'docker ps'
            }
        }

        stage('Build & Test Services') {
            stages {
                stage('UserService') {
                    steps {
                        dir('UserService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('ProductService') {
                    steps {
                        dir('ProductService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('CartService') {
                    steps {
                        dir('CartService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('InventoryService') {
                    steps {
                        dir('InventoryService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('NotificationService') {
                    steps {
                        dir('NotificationService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('OrderService') {
                    steps {
                        dir('OrderService') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('APIGateway') {
                    steps {
                        dir('APIGateway') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }

                stage('EurekaServer') {
                    steps {
                        dir('EurekaServer') {
                            bat 'mvn clean'
                            bat 'mvn compile'
                            bat 'mvn test'
                            bat 'mvn package -DskipTests'
                        }
                    }
                }
            }
        }

        stage('Start Final Application') {
            steps {
                echo 'Running docker-compose in project root to deploy all microservices...'
                bat 'docker-compose up -d'
                bat 'docker ps'
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
