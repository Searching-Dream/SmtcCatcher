# System Media Transport Controls Data Catcher

This is a Java Project for developers get Smtc(System Media Transport Controls) Data form Windows System Api By Java Native Interface

## What Data you can get

- Music Title
- Music Artist
- Total Music Time
- Current Music Position Time
- Current Music Played Progress
- Music Cover(Base64)
- Play State
- isChangedMusic(bool)
- Source Application Name

### how to use it

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