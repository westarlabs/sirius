FROM circleci/openjdk:8-jdk

USER root

RUN curl -sL https://deb.nodesource.com/setup_11.x | bash - \
 && apt-get install -y nodejs \
 && npm install -g solc \
 && ln -s /usr/bin/solcjs /usr/bin/solc

USER circleci