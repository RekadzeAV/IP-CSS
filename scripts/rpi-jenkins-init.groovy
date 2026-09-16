// Jenkins Initialization Script for Raspberry Pi 4
// This script configures Jenkins jobs and pipelines
// Date: 2026-07-08

import jenkins.model.*
import hudson.model.*
import hudson.plugins.parameterizedtrigger.*
import org.jenkinsci.plugins.workflow.job.*
import org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition

// Wait for Jenkins to be ready
Jenkins.instance.waitUntilReady(300, java.util.concurrent.TimeUnit.SECONDS)

println "=== Initializing Jenkins Configuration ==="

// Create credentials for NAS
def nasCredentials = [
    id: 'nas-smb-credentials',
    description: 'NAS SMB credentials for build artifacts',
    userName: 'VSCode',
    password: '$QN~snl0'.toCharArray()
]

// Create global pipeline libraries
def pipelineLibraries = """
// Global Pipeline Library for IP-CSS
def call(Map config) {
    pipeline {
        agent any
        
        options {
            buildDiscarder(logRotator(numToKeepStr: '30'))
            timeout(time: 2, unit: 'HOURS')
            disableConcurrentBuilds()
        }
        
        environment {
            NAS_HOST = '192.168.10.40'
            PROJECT_SHARE = 'AI-prodgect'
            BUILDS_SHARE = 'AI-prodgect-2'
            MOUNT_BASE = '/mnt/nas'
            BUILD_TYPE = config.buildType ?: 'unknown'
            DEPLOY_TARGET = config.deployTarget ?: 'test'
        }
        
        stages {
            stage('Checkout') {
                steps {
                    checkout scm
                }
            }
            
            stage('Mount NAS') {
                steps {
                    script {
                        sh '''
                            if ! mountpoint -q \${MOUNT_BASE}/\${PROJECT_SHARE}; then
                                sudo mount -t cifs //\${NAS_HOST}/\${PROJECT_SHARE} \${MOUNT_BASE}/\${PROJECT_SHARE} \\
                                    -o credentials=/home/andrey/.smbcredentials,iocharset=utf8,file_mode=0775,dir_mode=0775,noperm
                            fi
                            if ! mountpoint -q \${MOUNT_BASE}/\${BUILDS_SHARE}; then
                                sudo mount -t cifs //\${NAS_HOST}/\${BUILDS_SHARE} \${MOUNT_BASE}/\${BUILDS_SHARE} \\
                                    -o credentials=/home/andrey/.smbcredentials,iocharset=utf8,file_mode=0775,dir_mode=0775,noperm
                            fi
                        '''
                    }
                }
            }
            
            stage('Build') {
                steps {
                    script {
                        config.buildSteps.each { step ->
                            sh step
                        }
                    }
                }
            }
            
            stage('Test') {
                steps {
                    script {
                        if (config.testSteps) {
                            config.testSteps.each { step ->
                                sh step
                            }
                        }
                    }
                }
            }
            
            stage('Deploy Artifacts') {
                steps {
                    script {
                        def artifactDir = "\${MOUNT_BASE}/\${BUILDS_SHARE}/release/\${BUILD_TYPE}/\${DEPLOY_TARGET}"
                        sh "mkdir -p \${artifactDir}"
                        
                        config.artifacts.each { artifact ->
                            sh "cp -r ${artifact} \${artifactDir}/"
                        }
                        
                        // Create build info file
                        def buildInfo = """Build Information
==================
Build Number: \${BUILD_NUMBER}
Build Date: \$(date '+%Y-%m-%d %H:%M:%S')
Build Type: \${BUILD_TYPE}
Deploy Target: \${DEPLOY_TARGET}
Git Commit: \${GIT_COMMIT}
Git Branch: \${GIT_BRANCH}
""".stripIndent()
                        
                        writeFile file: "\${artifactDir}/build-info.txt", text: buildInfo
                    }
                }
            }
            
            stage('Cleanup') {
                steps {
                    script {
                        sh '''
                            # Unmount NAS shares
                            sudo umount \${MOUNT_BASE}/\${PROJECT_SHARE} || true
                            sudo umount \${MOUNT_BASE}/\${BUILDS_SHARE} || true
                        '''
                    }
                }
            }
        }
        
        post {
            success {
                echo "Build succeeded!"
            }
            failure {
                echo "Build failed!"
            }
            always {
                archiveArtifacts artifacts: '**/build-*.txt', allowEmptyArchive: true
                junit '**/test-results/**/*.xml' allowEmptyResults: true
            }
        }
    }
}
"""

// Save pipeline library
def libraryDir = new File(Jenkins.instance.getRootDir(), 'pipeline-libs')
libraryDir.mkdirs()
def libraryFile = new File(libraryDir, 'ipcss-build.groovy')
libraryFile.write(pipelineLibraries)

println "✓ Pipeline library created"

// Create build type configurations
def buildTypes = [
    'desktop-linux': [
        description: 'Desktop Linux build (x64)',
        buildSteps: [
            './gradlew :desktop:jvm:jar',
            './gradlew :desktop:jvm:test'
        ],
        testSteps: ['./gradlew :desktop:jvm:test'],
        artifacts: ['desktop/build/libs/*.jar', 'desktop/build/distributions/*.zip']
    ],
    'desktop-windows': [
        description: 'Desktop Windows build (x64)',
        buildSteps: [
            './gradlew :desktop:windows:jar',
            './gradlew :desktop:windows:test'
        ],
        testSteps: ['./gradlew :desktop:windows:test'],
        artifacts: ['desktop/build/libs/*.jar', 'desktop/build/distributions/*.exe']
    ],
    'desktop-macos': [
        description: 'Desktop macOS build (x64/ARM)',
        buildSteps: [
            './gradlew :desktop:macos:jar',
            './gradlew :desktop:macos:test'
        ],
        testSteps: ['./gradlew :desktop:macos:test'],
        artifacts: ['desktop/build/libs/*.jar', 'desktop/build/distributions/*.dmg']
    ],
    'android': [
        description: 'Android build (ARM64)',
        buildSteps: [
            './gradlew :android:assembleRelease',
            './gradlew :android:test'
        ],
        testSteps: ['./gradlew :android:test'],
        artifacts: ['android/app/build/outputs/**/*.apk', 'android/app/build/outputs/**/*.aab']
    ],
    'server': [
        description: 'Server build (Linux x64)',
        buildSteps: [
            './gradlew :server:jar',
            './gradlew :server:test'
        ],
        testSteps: ['./gradlew :server:test'],
        artifacts: ['server/build/libs/*.jar', 'server/build/distributions/*.zip']
    ],
    'native-libs': [
        description: 'Native libraries build',
        buildSteps: [
            './gradlew :native:build',
            './gradlew :native:test'
        ],
        testSteps: ['./gradlew :native:test'],
        artifacts: ['native/build/**/*.so', 'native/build/**/*.dylib', 'native/build/**/*.dll']
    ]
]

// Create Jenkins jobs for each build type
buildTypes.each { buildType, config ->
    def jobName = "IP-CSS-${buildType}"
    
    // Check if job already exists
    def existingJob = Jenkins.instance.getItem(jobName)
    if (existingJob) {
        println "Job ${jobName} already exists, skipping..."
        return
    }
    
    // Create pipeline job
    def job = new WorkflowJob(Jenkins.instance, jobName)
    
    // Configure job
    job.description = config.description
    job.addProperty(new ParametersDefinitionProperty([
        new StringParameterDefinition('DEPLOY_TARGET', 'test', 'Deploy target: test or prod')
    ]))
    
    // Set pipeline definition
    def pipelineScript = """
        @Library('ipcss-build') _
        ipcssBuild(
            buildType: '${buildType}',
            deployTarget: params.DEPLOY_TARGET,
            buildSteps: ${config.buildSteps},
            testSteps: ${config.testSteps ?: '[]'},
            artifacts: ${config.artifacts}
        )
    """
    
    job.definition = new CpsScmFlowDefinition([
        scriptPath: "Jenkinsfile-${buildType}",
        lightweight: true
    ])
    
    Jenkins.instance.add(job, job.name)
    println "✓ Created job: ${jobName}"
}

// Configure Jenkins global settings
Jenkins.instance.setNumExecutors(2)
Jenkins.instance.setLabelString('rpi4-arm64')
Jenkins.instance.save()

println ""
println "✅ Jenkins initialization completed!"
println ""
println "Created jobs:"
buildTypes.keySet().each { println "  - IP-CSS-${it}" }
println ""
println "Next steps:"
println "1. Configure SMB credentials in Jenkins"
println "2. Set up Git repository webhook"
println "3. Test build pipelines"
println "4. Configure email notifications"