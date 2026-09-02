# Keep Compose rules
-keepclassmembers class * extends androidx.compose.ui.node.Owner { *; }

# Keep ViewModel constructors
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
