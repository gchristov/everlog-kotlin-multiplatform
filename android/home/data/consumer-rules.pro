# Gson maps the home banner from Remote Config by reflection, so R8 mustn't rename or strip it
-keep class com.everlog.android.home.data.model.** { *; }
