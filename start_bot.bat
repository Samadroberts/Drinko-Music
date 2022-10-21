@ECHO OFF
SET YOUTUBE_USERNAME=""
SET YOUTUBE_PASSWORD=""
SET DISCORD_TOKEN=""

java -jar -Dorg.drinko.youtube.password=%YOUTUBE_PASSWORD% -Dorg.drinko.youtube.username=%YOUTUBE_USERNAME% -Ddiscord.api.token=%DISCORD_TOKEN% drinko-music-0.0.1-SNAPSHOT.jar
