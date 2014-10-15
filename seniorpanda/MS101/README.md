ms101-android
=============
"MS101.me":
-----------
Android app intended to provide better communication between doctors and patients with multiple
sclerosis through means of symptom tracking and medication adherence help.

About this repo:
----------------
I highly suggest using the program SourceTree as your git client, it provides excellent git-flow
management. Currently this repo only has the master branch though.

The IDE used to develop and maintain this app is Android Studio. There are a few key files that are
not kept in the repo so that the security of the app may be maintained. You will need to generate
your own if you intend to build off of what is here.

You will need to generate your own keystore file, create your own keystore.properties file, which should
look like the following (NO double quotes around anything!):  

storePassword=storePassword  
keyPassword=keyPassword  
keyAlias=keyAlias  
storeFile=filename.keystore  

and your own local.properties file, which should look like the following (This is the path where it is
on my system, yours may well be different since Android Studio will, by default, install the SDK inside
of its install directory.):  

sdk.dir=C\:\\android-sdk