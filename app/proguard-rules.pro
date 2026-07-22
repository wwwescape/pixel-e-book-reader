# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# Keep stack traces readable in crash reports.
-keepattributes SourceFile,LineNumberTable

# PDFBox-Android's JPXFilter references the Gemalto JP2 (JPEG2000) codec reflectively — it's an
# optional, separately-licensed dependency this app doesn't include, and PDFBox already handles
# its absence gracefully at runtime (JPEG2000 images just fail to decode). Safe to silence rather
# than pull in a codec we don't need for the other 6 supported formats.
-dontwarn com.gemalto.jp2.**
