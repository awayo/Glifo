# Glifo

![alt tag](http://i.imgur.com/ua29Un4.gif)

A library to map class fields between models an entities.


Gradle Installation
--------------------

You must use Jitpack.io
```
    repositories {
        jcenter()
        maven { url "https://jitpack.io" }
    }
```

Then add this dependency to your build.gradle file:
```
compile 'com.github.awayo:Glifo:v1.0.9'
```

Proguard Settings
------------------

If you use Proguard you need to keep your data classes

```
-keep class <app-package-name>.datasource.** { *; }
-keep class <app-package-name>.domain.** { *; }
```   
 
