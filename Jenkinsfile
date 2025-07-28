pipeline {
    agent any

    tools {
        jdk 'jdk-17'
    }

    environment {
        POSTGRES_USER = 'postgres'
        POSTGRES_PASSWORD = 'postgres'
        SPRING_DATASOURCE_USERNAME = 'postgres'
        SPRING_DATASOURCE_PASSWORD = 'postgres'
        EUREKA_URL = 'http://localhost:8761/eureka'
        KEYCLOAK_URL = 'http://localhost:8080'
        REDIS_HOST = 'localhost'
        REDIS_PORT = '6379'
        KAFKA_BROKER = 'localhost:29092'
    }

    stages {
        stage('Start Setup Dependencies') {
            steps {
                echo 'Running docker-compose in Setup folder to start dependencies...'
                dir('Setup') {
                    bat 'docker-compose up -d'
                }
                echo 'Waiting for PostgreSQL to be ready...'
                bat 'ping -n 10 127.0.0.1 > nul' // crude wait on Windows
                bat 'docker ps'
            }
        }

        stage('Build & Test Services') {
            stages {
                stage('UserService') {
                    steps {
                        dir('UserService') {
                            bat 'mvn clean compile -Dspring.profiles.active=default'
                            bat 'mvn test -Dspring.profiles.active=default'
                        }
                    }
                }

                // ... same for other services
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
