import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.util.Map.Entry;
import java.lang.Character.*;


public class ChatServer{

  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );



  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();


  // List with all used NickNames
  static private HashSet<String> nickList = new HashSet<>();


  // Hash Table with Client INformation associated with the respective socket
  static private Hashtable<SocketChannel, Client> clients = new Hashtable<>();



  // HashMap with rooms associated with clients info
  static private TreeMap<String, TreeSet<Client>> chatRooms = new TreeMap<>();

  // flags
  static private boolean chatLeave = false;

  static public void main( String args[] ) throws Exception {

    // Parse port from command line
    int port = Integer.parseInt( args[0] );

    try {

      // Instead of creating a ServerSocket, create a ServerSocketChannel
      ServerSocketChannel ssc = ServerSocketChannel.open();


      // Set it to non-blocking, so we can use select
      ssc.configureBlocking( false );


      // Get the Socket connected to this channel, and bind it to the
      // listening port
      ServerSocket ss = ssc.socket();
      InetSocketAddress isa = new InetSocketAddress( port );

      ss.bind( isa );

      // Create a new Selector for selecting
      Selector selector = Selector.open();

      // Register the ServerSocketChannel, so we can listen for incoming
      // connections
      ssc.register( selector, SelectionKey.OP_ACCEPT );
      System.out.println( "Listening on port "+port );



      while (true) {

        // See if we've had any activity -- either an incoming connection,
        // or incoming data on an existing connection
        int num = selector.select();

        // If we don't have any activity, loop around and wait again
        if (num == 0) {
          continue;
        }

        // Get the keys corresponding to the activity that has been
        // detected, and process them one by one
        Set<SelectionKey> keys = selector.selectedKeys();
        Iterator<SelectionKey> it = keys.iterator();

        while (it.hasNext()) {

          // Get a key representing one of bits of I/O activity
          SelectionKey key = it.next();


          // What kind of activity is it?
          if (key.isAcceptable()) {

            // It's an incoming connection.  Register this socket with
            // the Selector so we can listen for input on it
            Socket s = ss.accept();
            System.out.println( "Got connection from "+s );

            // Make sure to make it non-blocking, so we can use a selector
            // on it.
            SocketChannel sc = s.getChannel();
            sc.configureBlocking( false );


            // Register it with the selector, for reading
            sc.register( selector, SelectionKey.OP_READ );


            // If this client didn't exist yet, add him to the HashMap
            if(!clients.contains(sc)) clients.put(sc, new Client(sc));
            //sc.write(ByteBuffer.wrap("Bem Vindo\n".getBytes()));


          } else if (key.isReadable()) {
            SocketChannel sc = null;


            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc );


              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) { // tive de por algumas funcoes a retornar boolens para isto funcionar, dá para passar esta parte para baixo (func leaveChat) e voltar a por as funcoes a void

                key.cancel();

                Socket s = null;

                try {

                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();

                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }

                Client clientToRemove = clients.get(sc);

                // Remove the client socket
                clients.remove(sc);

                //remove the clients name
                String nick = clientToRemove.getClientNickName();

                if(nick != null)
                nickList.remove(nick);

                //remove the clients room
                String room = clientToRemove.getClientRoom();
                if(room != null)
                chatRooms.remove(room);

              }

            } catch( IOException ie ) {
              // On exception, remove this channel from the selector
              key.cancel();

              try {
                sc.close();

              } catch( IOException ie2 ) { System.out.println( ie2 ); }
              System.out.println( "Closed "+sc );

            }
          }
        }

        // We remove the selected keys, because we've dealt with them.
        keys.clear();
      }
    } catch( IOException ie ) {
      System.err.println( ie );
    }
  }


  // Just read the message from the socket and send it to stdout
  static private boolean processInput( SocketChannel sc ) throws IOException {

    // Read the message to the buffer
    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    Client client = clients.get(sc);

    boolean control = true;

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    } else {
      control = processMessage(client);
    }

    return control;
  }

  static private boolean processMessage(Client c){

    String comm = StandardCharsets.UTF_8.decode(buffer).toString();
    //System.out.println("Comm = "+comm);

    if(comm.charAt(0) == '/'){
      //System.out.println("Entrou em cima");
      return processCommand(c, comm);
    } else {
      publicMessage(c, comm);
      // Decode and print the message to stdout
      //String message = decoder.decode(buffer).toString();
      //System.out.print( message );
      System.out.println("Entrou em baixo");
    }
    return true;
  }

  static private boolean processCommand(Client c, String comm){

    String commInfo[] = comm.split(" ");

    switch (commInfo[0]){
      case "/nick":
        setNick(c, commInfo[1]);
        break;
      case "/join":
        /* FALTA TRATAR O NOME DA SALA
        if (commInfo[1] == null) {
          System.out.println("Entrou NO NULL");
          messageFromServerToClient(c, "ERROR\n");
        }
        */
        joinRoom(c, commInfo[1]);
        break;
      case "/leave":
        leaveRoom(c);
        break;
      case "/bye":
        leaveChat(c);
        return false;
      case "/priv":
        /*
        if (commInfo.length <= 3) {
          messageFromServerToClient(c, "ERROR1\n");
          System.out.println("length = " + commInfo.length);
          return true;
        }
        */
        privateMessage(c, commInfo[1], commInfo[2]);
        break;
      default:
        publicMessage(c, comm);
        break;
    }
    return true;
  }

/*
  static private String nomeSala(String sala){
    String novaSala=null;
    for (int i=0;i<sala.length();i++) {
      if (Character.isLetter(sala.charAt(i))) novaSala += sala.charAt(i);
    }
    if (novaSala == null) System.out.println("Nova sala igual a null");
    else System.out.println("Nova Sala = " + sala + "e size da nova sala = " + novaSala.length());
    return sala;
  }
*/


  static private void publicMessage(Client c, String message){
    if (c.getClientState() == Client.State.INIT || c.getClientState() == Client.State.OUTSIDE) {
      message = "ERROR: Public: Não está em nenhuma sala\n";
      messageFromServerToClient(c, message);
      return;
    }

    if (message.charAt(0) == '/') message = message.substring(1, message.length() - 1);

    /*MANDAR A MENSAGEM PARA OS CLIENTES DA SALA*/
    StringBuffer nick = new StringBuffer(c.getClientNickName());
    nick.deleteCharAt(nick.length()-1);
    message = "MESSAGE " + nick + " " + message+'\n';
    notifyClient(c.getClientRoom(), message);
  }

  static private void privateMessage(Client c, String nickRecetor, String mensagem){

    if(!nickList.contains(nickRecetor+'\n')){
      mensagem = "ERROR: Priv: O utilizador não existe\n";
      messageFromServerToClient(c, mensagem);
      return;
    }

    Collection<Client> clientes = clients.values();
    Iterator<Client> value = clientes.iterator();
    while(value.hasNext()){
     Client atual = value.next();
     if (atual.getClientNickName().equals(nickRecetor+'\n')) {
       StringBuffer nick = new StringBuffer(c.getClientNickName());
       nick.deleteCharAt(nick.length()-1);
       mensagem = "PRIVATE " + nick + " " + mensagem+'\n';
       messageFromServerToClient(atual, mensagem);
       break;
     }
    }
    mensagem = "OK\n";
    messageFromServerToClient(c, mensagem);
  }

  static private void messageFromServerToClient(Client c, String m){

    try {
      SocketChannel sc = c.getClienteSocket();
      sc.write(charset.encode(m));
    } catch(Exception e){
      e.printStackTrace(); // que é isto?
    }

  }

  static private void setNick(Client c, String newNick) {

      if(!nickList.contains(newNick)){

        if(c.getClientState() == Client.State.INIT){
          c.setClientState(Client.State.OUTSIDE);
        } else if (c.getClientState() == Client.State.INSIDE){
          // remove o nick antigo da lista
          nickList.remove(c.getClientNickName());

          StringBuffer nickAntigo = new StringBuffer(c.getClientNickName());
          nickAntigo.deleteCharAt(nickAntigo.length()-1);
          String nickChange = "NEWNICK " + nickAntigo + " " + newNick;

          // notificar os clientes da mesma sala (DA PARA SER MELHORADO AO INVES DE REMOVER E REENSERIR)
          removeClient(c);
          notifyClient(c.getClientRoom(), nickChange);
          chatRooms.get(c.getClientRoom()).add(c);
        } else { // estado igual a OUTSIDE
          nickList.remove(c.getClientNickName());
        }

        nickList.add(newNick);
        String nickChangedAproval = "OK\n";
        //String changeNickMessage = c.getClientNickName() + " has changed it's name to "+newNick+"\n"; //a preety message tem de ser organizada na parte do cliente
        //String nickFinal = nickChangedAproval + changeNickMessage;
        messageFromServerToClient(c, nickChangedAproval);
        c.setClientNickName(newNick);
        //BufferedWriter buf = new BufferedWriter(new OutputStreamWriter(sc.socket().getOutputStream()));
        //buf.write(nickFinal);
      } else messageFromServerToClient(c, "ERROR\n"); //messageFromServerToClient(c, "ERROR:Nick Changed: This nick already exists, choose another\n");
  }

  static private void joinRoom(Client c, String sala){

    if(c.getClientState() == Client.State.INIT){
      String message = "ERROR: You dont have a name yet, choose a nick first\n";
      messageFromServerToClient(c, message);
      return;
    }

    if(c.getClientState() == Client.State.OUTSIDE){
      if (chatRooms.get(sala) == null) { //criar a sala com este cliente
        System.out.println("Foi criada a sala " + sala);
        TreeSet<Client> clientes = new TreeSet<Client>();
        clientes.add(c);
        chatRooms.put(sala, clientes);
      } else { // adicionar o cliente caso a sala já exista
        /*NOTIFICAR OS CLIENTES*/
        String message = "JOINED " + c.getClientNickName();
        notifyClient(sala, message);
        /*INSERIR O CLIENTE NA SALA*/
        chatRooms.get(sala).add(c);
      }
      c.setClientRoom(sala);
      messageFromServerToClient(c,"OK\n");
      c.setClientState(Client.State.INSIDE);
    } else if(c.getClientState() == Client.State.INSIDE) {
      if(c.getClientRoom().equals(sala)){
        String message = "ERROR\n";
        messageFromServerToClient(c, message);
        return;
      } else {
        /*NOTIFICACAO DE SAIDA*/
        removeClient(c);
        String message = "LEFT " + c.getClientNickName();
        notifyClient(c.getClientRoom(), message);
        /*NOTIFICACAO DE ENTRADA*/
        if (chatRooms.get(sala) == null) { //criar a sala com este cliente
          System.out.println("Foi criada a sala " + sala);
          TreeSet<Client> clientes = new TreeSet<Client>();
          clientes.add(c);
          chatRooms.put(sala, clientes);
        } else {
          message = "JOINED " + c.getClientNickName();
          notifyClient(sala, message);
          chatRooms.get(sala).add(c); //adicionar à arvore
        }
        /*ALTERAR A SALA DO CLIENTE*/
        c.setClientRoom(sala);
        messageFromServerToClient(c,"OK\n");
      }
    }
  }

  static private void notifyClient(String sala, String message){
    TreeSet<Client> clientes = chatRooms.get(sala);
    Iterator<Client> value = clientes.iterator();
    while(value.hasNext()){
     //System.out.print(value.next().getClientNickName());
     Client atual = value.next();

     // Aqui tirei o \n por uma questao de estetica quando imprime,
     //acho que o \n nao é necessário pk o nome é concatenado no fim que ja tem o \n
     messageFromServerToClient(atual, message);
    }
  }

  static private void leaveRoom(Client c){
    if(c.getClientState() == Client.State.INIT || c.getClientState() == Client.State.OUTSIDE){
      String message = "ERROR: You aren't in a room yet, join a room first\n";
      messageFromServerToClient(c, message);
      return;
    } else {
      String message=null;
      if (chatLeave == false) {
        message = "OK\n";
        messageFromServerToClient(c, message);
      } else chatLeave = false;
      /*NOTIFICAR DE SAIDA*/
      String sala = c.getClientRoom();
      removeClient(c);
      c.setClientRoom(null);
      c.setClientState(Client.State.OUTSIDE);
      message = "LEFT " + c.getClientNickName();
      notifyClient(sala, message);
    }
  }

  static private void removeClient(Client c){
    if (c.compareTo(chatRooms.get(c.getClientRoom()).first()) == 0) {
      chatRooms.get(c.getClientRoom()).pollFirst(); //remover o 1 elem. da arvore
    } else
      chatRooms.get(c.getClientRoom()).remove(c); //remover da arvore
  }

  static private boolean leaveChat(Client c){

    String message = "BYE\n";
    messageFromServerToClient(c, message);

    if (c.getClientState() == Client.State.INSIDE) {
      chatLeave = true;
      leaveRoom(c);
    }
    /*
    try {
      Socket s = c.getClienteSocket().socket();
      System.out.println( "Closing connection to "+s );
      s.close();

    } catch( IOException ie ) {
      System.err.println( "Error closing socket "+c.getClienteSocket()+": "+ie );
    }

    // Remove the client socket
    clients.remove(c.getClienteSocket());

    //remove the clients name
    String nick = c.getClientNickName();

    if(nick != null) nickList.remove(nick);
    */
    return false;
    }

  // imprimir a lista de pessoas na sala
  /* ESQUECE ISTO PODE DAR JEITO NO FUTURO PARA NAO TAR A IR PESQUISAR OUTRA VEZ
  for (Map.Entry<String, TreeSet<Client>> entry : chatRooms.entrySet()) {
       System.out.println("Key: " + entry.getKey() + ". Value: " + entry.getValue().first().getClientNickName());
  }
  */

}
