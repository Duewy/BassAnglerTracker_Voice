# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# ====================================================
# Preserve line numbers for crash reports (Play Console)
# ====================================================
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ====================================================
# Gson — keep all fields that Gson reads/writes via reflection
# ====================================================
-keepattributes Signature
-keepattributes *Annotation*

# Keep Gson's own internals
-keep class com.google.gson.** { *; }
-dontwarn com.google.gson.**

# Keep YOUR data classes that Gson serializes
-keep class com.bramestorm.bassanglertracker.CatchItem { *; }
-keep class com.bramestorm.bassanglertracker.CatchItem$* { *; }
-keep class com.bramestorm.bassanglertracker.questionnaire.FirstTimeQuestionnaireAnswers { *; }
-keep class com.bramestorm.bassanglertracker.questionnaire.AdvertisingFocusProfile { *; }
-keep class com.bramestorm.bassanglertracker.questionnaire.QuestionnaireStore$* { *; }
-keep class com.bramestorm.bassanglertracker.models.** { *; }
-keep class com.bramestorm.bassanglertracker.voice.TournamentCatchStats { *; }

# Keep enums that Gson deserializes by name
-keepclassmembers enum com.bramestorm.bassanglertracker.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ====================================================
# Google Play Services & AdMob
# ====================================================
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.android.gms.**

# ====================================================
# AndroidX / Jetpack
# ====================================================
-dontwarn androidx.**
-keep class androidx.core.content.FileProvider { *; }