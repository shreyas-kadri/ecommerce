pipeline {
    agent any

    environment {
        COMPOSE_PROJECT_NAME = 'ecommerce'  // Optional: To namespace docker-compose
    }

    stages {

        stage('Checkout') {
            steps {
                echo 'Cloning repository...'
                checkout scm
            }
        }

        stage('Setup Dependencies') {
            steps {
                echo 'Starting dependency containers (Postgres, Kafka, Mongo, Keycloak, etc.)...'
                dir('Setup') {
                    sh 'docker compose up -d'
                }
                sh 'docker ps'
            }
        }

        stage('Build Microservices') {
            parallel {

                stage('Build UserService') {
                    steps {
                        dir('UserService') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build InventoryService') {
                    steps {
                        dir('InventoryService') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build CartService') {
                    steps {
                        dir('CartService') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build ProductService') {
                    steps {
                        dir('ProductService') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build NotificationService') {
                    steps {
                        dir('NotificationService') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build APIGateway') {
                    steps {
                        dir('APIGateway') {
                            sh 'mvn clean compile'
                        }
                    }
                }

                stage('Build EurekaServer') {
                    steps {
                        dir('EurekaServer') {
                            sh 'mvn clean compile'
                        }
                    }
                }
            }
        }

        stage('Test Microservices') {
            parallel {

                stage('Test UserService') {
                    steps {
                        dir('UserService') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test InventoryService') {
                    steps {
                        dir('InventoryService') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test CartService') {
                    steps {
                        dir('CartService') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test ProductService') {
                    steps {
                        dir('ProductService') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test NotificationService') {
                    steps {
                        dir('NotificationService') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test APIGateway') {
                    steps {
                        dir('APIGateway') {
                            sh 'mvn test'
                        }
                    }
                }

                stage('Test EurekaServer') {
                    steps {
                        dir('EurekaServer') {
                            sh 'mvn test'
                        }
                    }
                }
            }
        }

        stage('Package and Build Docker Images') {
            parallel {

                stage('Package UserService') {
                    steps {
                        dir('UserService') {
                            sh 'mvn package'
                            sh 'docker build -t user-service:latest .'
                        }
                    }
                }

                stage('Package InventoryService') {
                    steps {
                        dir('InventoryService') {
                            sh 'mvn package'
                            sh 'docker build -t inventory-service:latest .'
                        }
                    }
                }

                stage('Package CartService') {
                    steps {
                        dir('CartService') {
                            sh 'mvn package'
                            sh 'docker build -t cart-service:latest .'
                        }
                    }
                }

                stage('Package ProductService') {
                    steps {
                        dir('ProductService') {
                            sh 'mvn package'
                            sh 'docker build -t product-service:latest .'
                        }
                    }
                }

                stage('Package NotificationService') {
                    steps {
                        dir('NotificationService') {
                            sh 'mvn package'
                            sh 'docker build -t notification-service:latest .'
                        }
                    }
                }

                stage('Package APIGateway') {
                    steps {
                        dir('APIGateway') {
                            sh 'mvn package'
                            sh 'docker build -t api-gateway:latest .'
                        }
                    }
                }

                stage('Package EurekaServer') {
                    steps {
                        dir('EurekaServer') {
                            sh 'mvn package'
                            sh 'docker build -t eureka-server:latest .'
                        }
                    }
                }
            }
        }

        stage('Deploy Microservices') {
            steps {
                echo 'Starting all microservices using docker compose...'
                sh 'docker compose up -d'
                sh 'docker ps'
            }
        }

        stage('Health Check') {
            steps {
                echo 'Running health checks...'
                // Example: sh 'curl -f http://localhost:8080/actuator/health || exit 1'
            }
        }
    }

    post {
        always {
            echo 'Pipeline completed. You can add cleanup steps here if necessary.'
            // Example: sh 'docker compose down'
        }
    }
}
