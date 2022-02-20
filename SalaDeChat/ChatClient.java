import java.io.*;
import java.net.*;
import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;


public class ChatClient {

    // Variáveis relacionadas com a interface gráfica --- * NÃO MODIFICAR *
    JFrame frame = new JFrame("Chat Client");
    private JTextField chatBox = new JTextField();
    private JTextArea chatArea = new JTextArea();
    // --- Fim das variáveis relacionadas coma interface gráfica


    // Se for necessário adicionar variáveis ao objecto ChatClient, devem
    // ser colocadas aqui
    // A pre-allocated buffer for the received data
    static private ByteBuffer messageToServerBuffer = ByteBuffer.allocate( 16384 );

    // A pre-allocated buffer for the received data
    static private ByteBuffer messageFromServerBuffer = ByteBuffer.allocate( 16384 );
    private SocketChannel sc = null;


    // Decoder for incoming text -- assume UTF-8
    static private final Charset charset = Charset.forName("UTF8");
    static private final CharsetDecoder decoder = charset.newDecoder();


    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
      chatArea.append(message+ '\n');
    }


    // Construtor
    public ChatClient(String server, int port) throws IOException {
        // Inicialização da interface gráfica --- * NÃO MODIFICAR *
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(chatBox);
        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.SOUTH);
        frame.add(new JScrollPane(chatArea), BorderLayout.CENTER);
        frame.setSize(500, 300);
        frame.setVisible(true);
        chatArea.setEditable(false);
        chatBox.setEditable(true);
        chatBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    newMessage(chatBox.getText());
                } catch (IOException ex) {
                } finally {
                    chatBox.setText("");
                }
            }
        });

        frame.addWindowListener(new WindowAdapter() {
            public void windowOpened(WindowEvent e) {
                chatBox.requestFocusInWindow();
            }
        });
        // --- Fim da inicialização da interface gráfica


        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui
        InetSocketAddress sa = new InetSocketAddress(server,port);
        try{
        sc = SocketChannel.open(sa);
      }catch(Exception e){
        System.out.println("There is no server on that port");
        System.exit(0);
      }
    }

    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    // PREENCHER AQUI com código que envia a mensagem ao servidor
    public void newMessage(String message) throws IOException {
        if(message.charAt(1) != 'n') message += ' '; // quando nao é o nick fica o comando\n e nao entra no switch case (ex. /leave\n).
        message += '\n';
        sc.write(charset.encode(message));
    }





    // Método principal do objecto
    public void run() throws IOException {
        BufferedReader stream = new BufferedReader(new InputStreamReader((sc.socket().getInputStream())));
        String serverMgs;
        while(true){
          serverMgs = stream.readLine();
          String message[] = serverMgs.split(":");

          /* CASO SEJA PRECISO FECHAR O JFRAME DPS DO BYE
          if (message[0].equals("BYE")) {
            frame.dispose();
          }

          DEBUG
          if (message.length >= 3) {
            System.out.println("0 = " + message[0]);
            System.out.println("1 = " + message[1]);
            System.out.println("2 = " + message[2]);
          }
          */
          printMessage(message[0]); // tirei o \n pk acho que tava 1 a mais
        }
    }





    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }
}
