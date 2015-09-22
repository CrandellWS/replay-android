#!/bin/bash


# Fix the CircleCI path
function ensureDependenciesAndCreateAVD(){
  export PATH="$ANDROID_HOME/platform-tools:$ANDROID_HOME/tools:$PATH"

  DEPS="$ANDROID_HOME/installed-dependencies"

  if [ ! -e $DEPS ]; then
    echo y | android update sdk -u -a -t android-19 &&
    echo y | android update sdk -u -a -t platform-tools &&
    echo y | android update sdk -u -a -t build-tools-21.1.2 &&
    echo no | android create avd -n testAVD -f -t android-19 --abi default/armeabi-v7a &&
    touch $DEPS
  fi
}

function waitAVD {
    # http://blog.crowdint.com/2013/05/17/android-builds-on-travis-ci-with-maven.html
    (
    local bootanim=""
    export PATH=$(dirname $(dirname $(which android)))/platform-tools:$PATH
    until [[ "$bootanim" =~ "stopped" ]]; do
        sleep 5
        bootanim=$(adb -e shell getprop init.svc.bootanim 2>&1)
        echo "emulator status=$bootanim"
    done
    )
}

function sonatypePublish(){
  #http://benlimmer.com/2014/01/04/automatically-publish-to-sonatype-with-gradle-and-travis-ci/
  #http://www.survivingwithandroid.com/2014/05/android-guide-to-publish-aar-to-maven-gradle.html

  cd /home/ubuntu/replay-android/     # top dir of repo
  echo $PRIVATE_KEY > temppk.gpg      # pipe private key ascii into temp file
  tr -d \\n < temppk.gpg | tr \, \\n > privatekey.gpg   # 1) pipe temp file into 'tr' and remove any stray newlines 2) replace all commas with newlines
  gpg --allow-secret-key --import privatekey.gpg        # import newly-minted gpg file into gpg as a secret key
  rm -f temppk.gpg privatekey.gpg
  ./gradlew uploadArchives -PUSERNAME="${SONTAYPE_USERNAME}" -PPASSWORD="${SONATYPE_PASSWORD}" -Psigning.keyId=FDD99559 -Psigning.password=${SONATYPE_PASSWORD} -Psigning.secretKeyRingFile=/home/ubuntu/.gnupg/secring.gpg

  RETVAL=$?

  if [ $RETVAL -eq 0 ]; then
    echo ' Completed publish!'
  else
    echo 'Publish failed.'
    return 1
  fi

}
