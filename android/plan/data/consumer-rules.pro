# Firestore and Gson map the models by reflection, so R8 mustn't rename or strip them
-keep class com.everlog.android.plan.data.model.** { *; }
