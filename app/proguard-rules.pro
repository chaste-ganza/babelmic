# Add project specific ProGuard rules here.
# Keep ML Kit classes
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep BabelMic interfaces and implementations
-keep interface com.babelmic.app.core.** { *; }
-keep class com.babelmic.app.impl.** { *; }
