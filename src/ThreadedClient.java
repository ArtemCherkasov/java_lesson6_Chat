import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;


public class ThreadedClient {

    //protected static Logger log = LoggerFactory.getLogger(ThreadedClient.class);

    public static final int PORT = 19000;
    public static final String HOST = "localhost";
    private static final String EXIT = "exit";

    public static void main(String[] args) throws Exception {

        ThreadedClient client = new ThreadedClient();
        client.startClient();

    }

    public void startClient() {
        Socket socket = null;
        BufferedReader in = null;
        try {
            socket = new Socket(HOST, PORT);
            ConsoleThread console = new ConsoleThread(socket);
            console.start();

            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            String line = null;
            while ((line = in.readLine()) != null && !socket.isClosed()) {
                System.out.println(">> " + line);
            }

        } catch (Exception e) { 
            e.printStackTrace();
        } finally {
            Util.closeResource(in);
            Util.closeResource(socket);
        }
    }

    public void send(String message) {

    }

    class ConsoleThread extends Thread {
        BufferedReader console = new BufferedReader(new InputStreamReader(System.in));
        PrintWriter out;

        public ConsoleThread(Socket socket) throws Exception {
            out = new PrintWriter(socket.getOutputStream());
        }
        
        public void send(String message) throws InterruptedException{
        	out.println(message);
			out.flush();
            
        }

        @Override
        public void run() {
            try {
                String line;
                while (((line = console.readLine()) != null) && (!this.isInterrupted())) {
                    if (EXIT.equalsIgnoreCase(line)) {
                        //log.info("Closing chat");
                        System.out.println("Closing chat");
                        Thread.currentThread().interrupt();
                        break;
                    }
                    try {
						send(line);
					} catch (InterruptedException e) {
						// TODO: handle exception
						e.printStackTrace();
					}

                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Util.closeResource(out);
            }
        }

    }

}
