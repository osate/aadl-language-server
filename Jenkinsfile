/*******************************************************************************
 * Copyright (c) 2004-2026 Carnegie Mellon University and others. (see Contributors file).
 * All Rights Reserved.
 *
 * NO WARRANTY. ALL MATERIAL IS FURNISHED ON AN "AS-IS" BASIS. CARNEGIE MELLON UNIVERSITY MAKES NO WARRANTIES OF ANY
 * KIND, EITHER EXPRESSED OR IMPLIED, AS TO ANY MATTER INCLUDING, BUT NOT LIMITED TO, WARRANTY OF FITNESS FOR PURPOSE
 * OR MERCHANTABILITY, EXCLUSIVITY, OR RESULTS OBTAINED FROM USE OF THE MATERIAL. CARNEGIE MELLON UNIVERSITY DOES NOT
 * MAKE ANY WARRANTY OF ANY KIND WITH RESPECT TO FREEDOM FROM PATENT, TRADEMARK, OR COPYRIGHT INFRINGEMENT.
 *
 * This program and the accompanying materials are made available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 * SPDX-License-Identifier: EPL-2.0
 *
 * Created, in part, with funding and support from the United States Government. (see Acknowledgments file).
 *
 * This program includes and/or can make use of certain third party source code, object code, documentation and other
 * files ("Third Party Software"). The Third Party Software that is used by this program is dependent upon your system
 * configuration. By using this program, You agree to comply with any and all relevant Third Party Software terms and
 * conditions contained in any such Third Party Software or separate license file distributed with such Third Party
 * Software. The parties who own the Third Party Software ("Third Party Licensors") are intended third party beneficiaries
 * to this license with respect to the terms applicable to their Third Party Software. Third Party Software licenses
 * only apply to the Third Party Software and not any other portion of this program or this program as a whole.
 *******************************************************************************/

pipeline {
  agent any

  tools {
    jdk 'OpenJDK21'
  }

  options {
    disableConcurrentBuilds()
    buildDiscarder(logRotator(numToKeepStr: '20', artifactNumToKeepStr: '10'))
  }

  stages {
    stage('Initialize OSATE') {
      steps {
        sh 'git submodule sync -- osate2'
        sh 'git submodule update --init --depth 1 osate2'
      }
    }

    stage('Build and Test') {
      steps {
        withMaven(maven: 'M3', mavenLocalRepo: '.repository', publisherStrategy: 'EXPLICIT') {
          wrap([$class: 'Xvnc', takeScreenshot: false, useXauthority: true]) {
            sh './scripts/build-test-release'
          }
        }
      }
    }

    stage('Deploy') {
      when {
        expression {
          env.BRANCH_NAME == 'main' || env.GIT_BRANCH == 'main' || env.GIT_BRANCH == 'origin/main'
        }
      }
      steps {
        sh 'bash ./deploy.sh'
      }
    }
  }

  post {
    always {
      junit '**/target/surefire-reports/*.xml'
    }
    success {
      archiveArtifacts artifacts: 'releng/org.osate.aadl.ls.repository/target/repository/**/*,releng/org.osate.aadl.ls.repository/target/*.zip,target/build-provenance.properties',
          fingerprint: true
    }
  }
}
