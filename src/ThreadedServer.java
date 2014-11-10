import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ThreadedServer {

    //protected static Logger log = LoggerFactory.getLogger("ThreadedServer");
    private static final int PORT = 19000;
    private static int counter = 0;
    private ExecutorService service;

    // список обработчиков для клиентов
    private List<ClientHandler> handlers = new ArrayList<>();

    public static void main(String[] args) throws Exception {
        ThreadedServer server = new ThreadedServer();
        server.startServer();
    }

    public void startServer() throws Exception {
        //log.info("Starting server...");
        System.out.println("Starting server...");
        ServerSocket serverSocket = new ServerSocket(PORT);
        service = Executors.newFixedThreadPool(2);
        while (true) {

            // блокируемся и ждем клиента
            Socket socket = serverSocket.accept();
            //log.info("Client connected: " + socket.getInetAddress().toString() + ":" + socket.getPort());
            System.out.println("Client connected: " + socket.getInetAddress().toString() + ":" + socket.getPort());

            // создаем обработчик
            ClientHandler handler = new ClientHandler(this, socket, counter++);
            service.submit(handler);
        	handlers.add(handler);
        }
    }

    /*
    Для каждого присоединившегося пользователя создается поток обработки независимый от остальных
     */

    class ClientHandler extends Thread {

        private ThreadedServer server;
        private BufferedReader in;
        private PrintWriter out;
        private String login = "";

        // номер, чтобы различать потоки
        private int number;

        public ClientHandler(ThreadedServer server, Socket socket, int counter) throws Exception{
            this.server = server;
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            out = new PrintWriter(socket.getOutputStream());
            number = counter;
        }

        // Отправка сообщения в сокет, связанный с клиентом
        public void send(String message) throws InterruptedException{
        	out.println(message);
			out.flush();
            
        }

        @Override
        public void run(){

            // В отдельном потоке ждем данных от клиента
            try {
                String line = null;
                while (((line = in.readLine()) != null) && (!this.isInterrupted())) {
                	try {
                		System.out.println("Handler[" + number + "]<< " + line);
                        String[] parse_line = line.split(" ");
                        
                        switch (parse_line[0]) {
    					case ".help":
    						
    						if(parse_line.length == 1){
    							send(".login [name]- залогиниться с именем, пока не залогинились, сообщения не получаем\n" +
    									".help — вывод списка команд\n" +
    									".private <user_to> — отправить приватное сообщение user_to\n" + 
    									".exit — выйти из чата"
    								);
    						} else {
    							send("for help use \".help\" without any parameters");
    						}

    						break;
    					case ".exit":
    						if(parse_line.length == 1){
    							Util.closeResource(in);
    			                Util.closeResource(out);
    							Thread.currentThread().interrupt();
    						} else {
    							send("for exit use \".exit\" without any parameters");
    						}
    						
    						break;
    						
    					case ".private":
    						
    						for(ClientHandler handler: handlers){
    							if(handler.login.compareTo(parse_line[1]) == 0){
    								StringBuilder message = new StringBuilder();
    								message.append("private message from "  + login + ":");
    								for(int i = 2; i < parse_line.length; ++i){
    									message.append(" ");
    									message.append(parse_line[i]);
    								}
    								handler.send(message.toString());
    								send(message.toString());
    							}
    						}
    						
    						break;

    					default:
    						if (login != "") {
    							server.broadcast(login + ": " + line);
    						} else	{
    	                    	if(parse_line[0].compareTo(".login") == 0 && parse_line.length == 2){
    	                    		login = parse_line[1];
    	                    	} else {
    	                    		send("invalide command, use \".login [name]\"");
    	                    	}
    	                    }
    						break;
    					}
                        
					} catch (InterruptedException e) {
						// TODO: handle exception
						Util.closeResource(in);
		                Util.closeResource(out);
					} 
                	
                }

            } catch (IOException e) {
                //log.error("Failed to read from socket");
                System.out.println("Failed to read from socket");
            } 
            handlers.remove(this);
        }
    }

    // рассылаем всем подписчикам
    public void broadcast(String msg) throws InterruptedException {
        //log.info("Broadcast to all: " + msg);
        System.out.println("Broadcast to all: " + msg);
        for (ClientHandler handler : handlers) {
            if (handler.login != "") handler.send(msg);
        }

    }

}
