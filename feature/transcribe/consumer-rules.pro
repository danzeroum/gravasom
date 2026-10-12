# Regras de consumer para quem depender deste modulo.
# O JNI chama metodos por nome (WhisperBridge.nativeInit/nativeFree/nativeTranscribe):
# R8 precisa manter a classe e os metodos nativos.
-keep class com.gravasom.feature.transcribe.model.WhisperBridge { *; }
-keepclasseswithmembernames class * { native <methods>; }
