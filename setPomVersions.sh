# With this command you can set a new version in all pom.xml files.
# Run it in the root folder of your project
#
# usage in Terminal:  ./setPomVersions.sh 1.0.0-SNAPSHOT
#
# after this run a clean build and commit the version change
#
./mvnw versions:set -DgenerateBackupPoms=false -DnewVersion=$1
