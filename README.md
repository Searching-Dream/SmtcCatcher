# System Media Transport Controls Data Catcher

This is a Java Project for developers get Smtc(System Media Transport Controls) Data form Windows System Api By Java Native Interface

## What Data you can get

- Music Title
- Music Artist
- Total Music Time
- Current time position of music
- Current Music Played Progress
- Music Cover(Base64)
- Playing statements
- isChangedMusic(bool)
- Source Application Name

## how to use it

```java
public class Main {
    public static Loader loader;

    public static void main(String[] args) {
        loader = Loader.startSmtc();
        System.out.println("SMTC Loader Started\n");
        while (true) {
            System.out.println(loader.getCurrentMediaInfo().toString());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
```

You can use it to print what data you can get

---
## ⚠️ ⚠️⚠️
## Do not attempt to change the package name of Loader.java or the method names in Loader, as this may cause the Java Native Interface to fail. Including when using obfuscation tools like ProGuard, be careful with renaming classes and methods that are referenced by native code.
## ⚠️⚠️⚠️
---