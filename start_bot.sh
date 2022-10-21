export YOUTUBE_USERNAME=""
export YOUTUBE_PASSWORD=""
export DISCORD_TOKEN=""

java -jar -Dorg.drinko.youtube.password=$YOUTUBE_PASSWORD -Dorg.drinko.youtube.username=$YOUTUBE_USERNAME -Ddiscord.api.token=$DISCORD_TOKEN drinko-music-0.0.1-SNAPSHOT.jar
