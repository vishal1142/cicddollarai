# Use current LTS so jenkins-plugin-cli can resolve latest plugin lines in plugins.txt
# (lts-jdk11 was 2.462.x and no longer satisfies transitive plugin requirements.)
FROM jenkins/jenkins:lts-jdk17

USER root
RUN mkdir -p /usr/share/jenkins/ref/casc \
    && chown -R jenkins:jenkins /usr/share/jenkins/ref/casc

USER jenkins

COPY --chown=jenkins:jenkins plugins.txt /usr/share/jenkins/ref/plugins.txt
RUN jenkins-plugin-cli --plugin-file /usr/share/jenkins/ref/plugins.txt

COPY --chown=jenkins:jenkins jenkins.yaml /usr/share/jenkins/ref/casc/jenkins.yaml

ENV CASC_JENKINS_CONFIG=/var/jenkins_home/casc
