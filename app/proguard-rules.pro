# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile
# Suppress warnings for desktop/server libraries not available on Android
# These classes are referenced by dependencies but never used at runtime on Android
-dontwarn java.awt.Color
-dontwarn java.awt.color.ColorSpace
-dontwarn java.awt.geom.AffineTransform
-dontwarn java.awt.geom.Dimension2D
-dontwarn java.awt.geom.Path2D
-dontwarn java.awt.geom.PathIterator
-dontwarn java.awt.geom.Point2D
-dontwarn java.awt.geom.Rectangle2D$Double
-dontwarn java.awt.geom.Rectangle2D
-dontwarn java.awt.image.BufferedImage
-dontwarn java.awt.image.ColorModel
-dontwarn java.awt.image.ComponentColorModel
-dontwarn java.awt.image.DirectColorModel
-dontwarn java.awt.image.IndexColorModel
-dontwarn java.awt.image.PackedColorModel
-dontwarn javax.xml.stream.Location
-dontwarn javax.xml.stream.XMLStreamException
-dontwarn javax.xml.stream.XMLStreamReader
-dontwarn net.sf.saxon.Configuration
-dontwarn net.sf.saxon.dom.DOMNodeWrapper
-dontwarn net.sf.saxon.om.Item
-dontwarn net.sf.saxon.om.NodeInfo
-dontwarn net.sf.saxon.om.Sequence
-dontwarn net.sf.saxon.om.SequenceTool
-dontwarn net.sf.saxon.sxpath.IndependentContext
-dontwarn net.sf.saxon.sxpath.XPathDynamicContext
-dontwarn net.sf.saxon.sxpath.XPathEvaluator
-dontwarn net.sf.saxon.sxpath.XPathExpression
-dontwarn net.sf.saxon.sxpath.XPathStaticContext
-dontwarn net.sf.saxon.sxpath.XPathVariable
-dontwarn net.sf.saxon.tree.wrapper.VirtualNode
-dontwarn net.sf.saxon.value.DateTimeValue
-dontwarn net.sf.saxon.value.GDateValue
-dontwarn org.apache.batik.anim.dom.SAXSVGDocumentFactory
-dontwarn org.apache.batik.bridge.BridgeContext
-dontwarn org.apache.batik.bridge.DocumentLoader
-dontwarn org.apache.batik.bridge.GVTBuilder
-dontwarn org.apache.batik.bridge.UserAgent
-dontwarn org.apache.batik.bridge.UserAgentAdapter
-dontwarn org.apache.batik.util.XMLResourceDescriptor
-dontwarn org.osgi.framework.Bundle
-dontwarn org.osgi.framework.BundleContext
-dontwarn org.osgi.framework.FrameworkUtil
-dontwarn org.osgi.framework.ServiceReference

# Keep all fragments and their newInstance methods
-keep class * extends androidx.fragment.app.Fragment {
    public <init>(...);
}

# Keep companion object methods for fragments (especially newInstance)
-keepclassmembers class * extends androidx.fragment.app.Fragment {
    public static ** newInstance(...);
}

# Keep fragment companion objects
-keep class * extends androidx.fragment.app.Fragment$Companion {
    *;
}

# Keep CompletionFragment specifically to ensure it's not stripped
-keep class com.lkacz.pola.CompletionFragment {
    *;
}
-keep class com.lkacz.pola.CompletionFragment$Companion {
    *;
}

# Keep Logger class to prevent obfuscation issues
-keep class com.lkacz.pola.Logger {
    *;
}
-keep class com.lkacz.pola.Logger$Companion {
    *;
}

# Keep MainActivity callbacks
-keepclassmembers class com.lkacz.pola.MainActivity {
    private void onProtocolCompleted();
}

# Keep critical singleton objects that are accessed throughout the app
-keep class com.lkacz.pola.Prefs {
    *;
}
-keep class com.lkacz.pola.ColorManager {
    *;
}
-keep class com.lkacz.pola.ParsingUtils {
    *;
}
-keep class com.lkacz.pola.TransitionManager {
    *;
}

# Keep listener interfaces to prevent issues with callbacks
-keep interface com.lkacz.pola.StartFragment$OnProtocolSelectedListener {
    *;
}

# Keep Service classes
-keep class * extends android.app.Service {
    *;
}

# Keep FragmentLoader to ensure protocol parsing works correctly
-keep class com.lkacz.pola.FragmentLoader {
    *;
}

# Keep ProtocolManager
-keep class com.lkacz.pola.ProtocolManager {
    *;
}
-keep class com.lkacz.pola.ProtocolManager$Companion {
    *;
}

# Keep all coroutine-related classes to prevent initialization failures
-keep class kotlinx.coroutines.** { *; }
-dontwarn kotlinx.coroutines.**
-keepclassmembers class kotlinx.coroutines.** { *; }

# Keep Kotlin metadata
-keep class kotlin.Metadata { *; }

# Keep all lifecycle-related classes
-keep class androidx.lifecycle.** { *; }
-keepclassmembers class androidx.lifecycle.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel {
    *;
}

# Keep all our app classes to prevent ANY R8 issues (temporary for debugging)
-keep class com.lkacz.pola.** { *; }
-keepclassmembers class com.lkacz.pola.** { *; }

# Keep AlertDialog and related classes
-keep class androidx.appcompat.app.AlertDialog { *; }
-keep class androidx.appcompat.app.AlertDialog$Builder { *; }
-keepclassmembers class androidx.appcompat.app.AlertDialog$Builder { *; }
-keep class android.content.DialogInterface { *; }
-keep class android.content.DialogInterface$OnClickListener { *; }

# Keep Apache POI (Excel library) classes
-keep class org.apache.poi.** { *; }
-keepclassmembers class org.apache.poi.** { *; }
-dontwarn org.apache.poi.**
-dontwarn java.awt.Shape

# Keep XML-related classes that POI depends on
-keep class org.apache.xmlbeans.** { *; }
-dontwarn org.apache.xmlbeans.**
-keep class org.openxmlformats.schemas.** { *; }
-dontwarn org.openxmlformats.schemas.**
