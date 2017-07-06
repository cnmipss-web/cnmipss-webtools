FROM java:8-alpine
MAINTAINER Your Name <you@example.com>

ADD target/uberjar/certification-db.jar /certification-db/app.jar

EXPOSE 3000

CMD ["java", "-jar", "/certification-db/app.jar"]
