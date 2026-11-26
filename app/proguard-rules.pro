# R8 rules for the release build.
#
# Room, kotlinx.serialization, Compose and Navigation 3 ship their own consumer
# rules, so nothing is needed here yet. Add a rule only for a crash seen in a
# release build, with a comment saying why.

# Keep line numbers so crash reports in Play Console (Android vitals) point at
# real lines once the mapping file is uploaded; hide the source file names.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
