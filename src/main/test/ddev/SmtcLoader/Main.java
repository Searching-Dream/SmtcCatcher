package ddev.SmtcLoader;

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