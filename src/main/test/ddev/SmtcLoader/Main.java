package ddev.SmtcLoader;

import java.util.Scanner;

public class Main {
    public static Loader loader;

    public static void main(String[] args) {
        loader = Loader.startSmtc();
        System.out.println("SMTC Loader Started\n");
        Scanner scanner = new Scanner(System.in);

        while (true) {
            String command = scanner.nextLine();
            System.out.println(command);
            switch (command.toLowerCase()) {
                case "pause":
                    loader.pause();
                    break;
                case "play":
                    loader.play();
                    break;
                case "next":
                    loader.next();
                    break;
                case "previous":
                    loader.previous();
                    break;
                case "stop":
                    loader.stop();
                    break;
                case "toggle":
                    loader.togglePlayPause();
                    break;
            }
//            System.out.println(loader.getCurrentMediaInfo().toString());
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            }
        }
    }
}