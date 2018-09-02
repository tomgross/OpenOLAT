FROM tomcat:8.5-jre8

ENV OO_VER=1242
ENV CATALINA_HOME=/usr/local/tomcat

COPY docker/olat-config/ROOT.xml $CATALINA_HOME/conf/Catalina/localhost/
COPY docker/olat-config/lib/* $CATALINA_HOME/lib/
COPY target/openolat-lms-13.0-SNAPSHOT.war /tmp/openolat.war

RUN mkdir -p /home/openolat/logs \
  && rm -rf $CATALINA_HOME/webapps/* \
#  && wget -q http://www.openolat.org/fileadmin/downloads/releases/openolat_$OO_VER.war -O /tmp/openolat.war \
  && unzip -qd /home/openolat/webapp /tmp/openolat.war \
  && rm /tmp/openolat.war

