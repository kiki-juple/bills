# Every dependency (kotlinx.serialization, Ktor + OkHttp, SQLDelight, CameraX, Compose) ships
# its own consumer rules, so nothing here keeps code. Add a rule only for what R8 can't trace,
# like reflection or a class named in a string, and test the release build after.

# Readable release stack traces; retrace them with build/outputs/mapping/release/mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile
