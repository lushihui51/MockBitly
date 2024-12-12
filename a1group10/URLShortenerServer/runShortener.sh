mkdir -p /virtual/$USER
rm -f /virtual/$USER/urlshortener.db
sqlite3 /virtual/$USER/urlshortener.db < schema.sql

javac URLShortener.java
java -classpath ".:sqlite-jdbc-3.39.3.0.jar" URLShortener &
