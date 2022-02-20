import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;

public class Client implements Comparable<Client> {

  String clientNickName;
  String clientRoom;
  State clientState;
  SocketChannel channel;

  static enum State {
    INIT, OUTSIDE, INSIDE
  }

  public Client(SocketChannel sc){
    this.clientState = State.INIT;
    this.clientNickName = null;
    this.clientRoom = null;
    this.channel = sc;
  }

  public String getClientNickName(){
    return clientNickName;
  }

  public void setClientNickName(String newNick){
    clientNickName = newNick;
  }

  public String getClientRoom(){
    return clientRoom;
  }

  public void setClientRoom(String newRoom){
    clientRoom = newRoom;
  }

  public State getClientState(){
    return clientState;
  }

  public void setClientState(State newState){
    clientState = newState;
  }

  public SocketChannel getClienteSocket(){
    return channel;
  }

  @Override
  public int compareTo (Client obj){ // nao sei se este metodo esta correto foi so para corrigr um erro (mas acho que sim)
      if (this.clientNickName == obj.clientNickName) return 0;
      else return 1;
  }


}
