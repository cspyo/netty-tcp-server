./gradlew clean
./gradlew build -x test
cd build/libs
java -jar netty-0.0.1-SNAPSHOT.jar