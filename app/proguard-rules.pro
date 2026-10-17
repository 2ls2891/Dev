# kotlinx.serialization 과 Google Mobile Ads SDK 는 자체 consumer 규칙을 포함한다.
# 아래는 @Serializable 콘텐츠/저장 모델이 난독화로 깨지지 않도록 하는 보강 규칙이다.
-keepclassmembers @kotlinx.serialization.Serializable class nonogram.** {
    *** Companion;
    *** INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class nonogram.**$$serializer { *; }
