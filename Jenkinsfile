node {
    stage('Build tooling') {
        env.JAVA_HOME = "${tool 'jdk8'}"
        env.PATH = "${env.JAVA_HOME}/bin:${env.PATH}"
        mvnHome = tool 'maven3'
        env.MAVEN_OPTS = "-Xmx512m -XX:MaxPermSize=128m"
    }

    stage('Checkout') {
        checkout scm

        LONG_GIT_COMMIT = sh(returnStdout: true, script: 'git rev-parse HEAD').trim()
        GIT_COMMIT = LONG_GIT_COMMIT.take(8)
        GIT_BRANCH = env.BRANCH_NAME
    }

    stage('Maven build') {
        try {
            sh "${mvnHome}/bin/mvn -C clean package -Dbuild.number=jenkins-${env.BUILD_NUMBER}-${GIT_BRANCH}-${GIT_COMMIT}"
            // step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
        } catch(err) {
            // step([$class: 'JUnitResultArchiver', testResults: '**/target/surefire-reports/TEST-*.xml'])
            throw err
        }
    }

    stage('Archive artifacts') {
        archiveArtifacts artifacts: '**/target/*.jar, **/target/*.war', fingerprint: true, onlyIfSuccessful: true
    }

    stage('Sonar analysis') {
        withSonarQubeEnv('sonar_6.1') {
            sh "${mvnHome}/bin/mvn -C sonar:sonar -Dbuild.number=jenkins-${env.BUILD_NUMBER}-${GIT_BRANCH}-${GIT_COMMIT} -Dsonar.branch=${GIT_BRANCH}"
        }
    }
}
