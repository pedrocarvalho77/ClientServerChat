import java.io.*;
import java.net.*;
import java.nio.*;
import java.nio.channels.*;
import java.nio.charset.*;
import java.util.*;
import java.lang.*;

class SocketInfo {
  int state; //1=init 2=outside 3=inside
  String nick;
  String room;
  SocketChannel sc;

  public SocketInfo(SocketChannel sc) {
    this.sc = sc;
    this.nick = "";
    this.room = "";
    this.state = 1;
  }
}
public class ChatServer
{
  // A pre-allocated buffer for the received data
  static private final ByteBuffer buffer = ByteBuffer.allocate( 16384 );

  // Decoder for incoming text -- assume UTF-8
  static private final Charset charset = Charset.forName("UTF8");
  static private final CharsetDecoder decoder = charset.newDecoder();
  static private final CharsetEncoder enc = Charset.forName("US-ASCII").newEncoder();
  //LinkedList<String> nicks = new LinkedList<String>();
  static LinkedList<SocketInfo> sockets = new LinkedList<SocketInfo>();


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
            sc.register( selector, SelectionKey.OP_READ);

          } else if (key.isReadable()) {

            SocketChannel sc = null;

            try {

              // It's incoming data on a connection -- process it
              sc = (SocketChannel)key.channel();
              boolean ok = processInput( sc );

              // If the connection is dead, remove it from the selector
              // and close it
              if (!ok) {
                key.cancel();

                Socket s = null;
                try {
                  s = sc.socket();
                  System.out.println( "Closing connection to "+s );
                  s.close();
                } catch( IOException ie ) {
                  System.err.println( "Error closing socket "+s+": "+ie );
                }
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
    SocketInfo si = new SocketInfo(sc);

    buffer.clear();
    sc.read( buffer );
    buffer.flip();

    // If no data, close the connection
    if (buffer.limit()==0) {
      return false;
    }

    // Decode and print the message to stdout
    String message = decoder.decode(buffer).toString();
    message = removeLine(message);
    int tmp; // tmp vai ser utilizado para o indice da lista que ta a socket pretendida
    String s = cutMessage(0,message); //deve retornar o comando
    //System.out.println(message);
    if(s.equals("/nick"))  {
     // System.out.println("nick");
      if (message.length()<=6 || (Character.isLetter(message.charAt(6))== false && Character.isDigit(message.charAt(6))== false)) {
        sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
      }
      else {
        s = cutMessage(1,message); // deve retornar o nick escolhido
        if(searchNick(sockets,s)) sc.write(enc.encode(CharBuffer.wrap("ERROR\n"))); //envia error, precisa de \n
        else {
          tmp = searchSocket(sockets,sc);
          if (tmp != -1 ) si = sockets.remove(tmp);
          if(tmp == -1 || si.state==2) {
            sc.write(enc.encode(CharBuffer.wrap("OK\n")));
            si.state=2;
            si.nick=s;
            sockets.add(si);
          }
          else {
            if(si.state==3) {
              sc.write(enc.encode(CharBuffer.wrap("OK\n")));
              messageRoom(sockets,"NEWNICK " + si.nick + " " + s + "\n", si.room);
              si.state=3;
              si.nick=s;
            }
          }
        }
      }
    }

    else {
      if(s.equals("/join")) {         
       // System.out.println("join");
        if (message.length()<=6 || (Character.isLetter(message.charAt(6))== false && Character.isDigit(message.charAt(6))== false)) {
          sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
        }
        else {
          s = cutMessage(1,message); // deve retornar a sala 
          tmp = searchSocket(sockets,sc);
          if (tmp==-1) { sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));} // o professor nao disse nada se tiver no init e der join
          //assumo que deve mandar erro para o cliente
          else {
            si = sockets.remove(tmp);
            if (si.state==2) {
              si.state=3;
              si.room=s;
              sc.write(enc.encode(CharBuffer.wrap("OK\n")));
              messageRoom(sockets,"JOINED " + si.nick + "\n", s);
              sockets.add(si);
            }
            else {
              if (si.state==3) {
                si.state=3;
                messageRoom(sockets,"LEFT " + si.nick + "\n", si.room);
                si.room=s;
                sc.write(enc.encode(CharBuffer.wrap("OK\n")));
                messageRoom(sockets,"JOINED " + si.nick + "\n", s);
              }
            }
          }
        }
      }

      else {
        if(s.equals("/leave")) {
         // System.out.println("leave");
          tmp = searchSocket(sockets,sc);
          if (tmp==-1) { sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));}
          else {
            si = sockets.remove(tmp);
            if (si.state==2) {
              sc.write(enc.encode(CharBuffer.wrap("ERROR\n"))); // quanto ta no outside, leave da erro?
              sockets.add(si);
            }
            if (si.state==3) {
              si.state = 2;
              messageRoom(sockets,"LEFT " + si.nick + "\n", si.room);
              si.room="";
            }
          }
        }

        else {
          if(s.equals("/bye")) {
           // System.out.println("bye");
            tmp = searchSocket(sockets,sc);
            if (tmp==-1) { sc.write(enc.encode(CharBuffer.wrap("BYE\n")));}
            else {
              si = sockets.remove(tmp);
              if (si.state==2) {
                sc.write(enc.encode(CharBuffer.wrap("BYE\n")));
                sc.close();
              }
              if (si.state==3) {
                sc.write(enc.encode(CharBuffer.wrap("BYE\n")));
                messageRoom(sockets,"LEFT " + si.nick + "\n", si.room);
                sc.close();
              }
            }
          }
          
          else{
            if(!s.equals("/priv") || !searchNick(sockets,cutMessage(1,message))) {       
              sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
            }
            else {
              si = sockets.remove(tmp);
              if (si.state==3) {
                sockets.add(si);
                int tmp = searchSocket(sockets, cutMessage(1,message));
                for(int i=0; i<tmp;i++){
                  si = l.get(i);
                  SocketChannel sc = si.sc;
                }
                sc.write(enc.encode(CharBuffer.wrap("OK\n")));
                sc.write(enc.encode(CharBuffer.wrap(message)));
                //messageRoom(sockets,"MESSAGE " + si.nick + " " + message + "\n", si.room);
              }
              else {
                sockets.add(si);
                sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
              }
            }
            
            else {
              System.out.println("message");
              tmp = searchSocket(sockets,sc);
              if (tmp==-1) { sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));}
              else {
                si = sockets.remove(tmp);
                if (si.state==3) {
                  sockets.add(si);
                  messageRoom(sockets,"MESSAGE " + si.nick + " " + message + "\n", si.room);
                }
                else {
                  sockets.add(si);
                  sc.write(enc.encode(CharBuffer.wrap("ERROR\n")));
                }
              }
            }
          }
        }
      }
    }
    return true;
  }

  static private String cutMessage(int x, String s) { //retorna o argumento x 
     String[] sS = s.split(" ");
    // System.out.println("cutMessage " + sS[x]);
     return sS[x];
  }

  static private String removeLine(String s) {
      String[] sS = s.split("\n");
      //System.out.println("cutMessage " + sS[x]);
      return sS[0];
  }

  static private boolean searchNick(LinkedList<SocketInfo> l, String s) { //procura na lista sockets pela socket que tem o nick (s) associado
    int size = l.size();
    SocketInfo si;
    for (int i=0;i<size;i++) {
      si = l.get(i);
      if (s.equals(si.nick)) return true;
    }
    return false;
  }

  static private int searchSocket(LinkedList<SocketInfo> l, SocketChannel sc) { //procura na lista sockets pela socket sc, retorna o indice
    int size = l.size();
    SocketInfo si;
    for (int i=0;i<size;i++) {
      si = l.get(i);
      if (si.sc == sc) {
       // System.out.println("THIS IS SOCKET " + i);
        return i;
      }
    }
   // System.out.println("THIS IS SOCKET -1");
    return -1;
  }


  static private void messageRoom(LinkedList<SocketInfo> l, String s, String room) { // manda mensagem que tao na mesma sala
    SocketInfo si;
    int size = l.size();
    for (int i=0;i<size;i++) {
      si = l.get(i);
      SocketChannel sc = si.sc;
      if (room.equals(si.room)) {
        try {
          sc.write(enc.encode(CharBuffer.wrap(s)));
        }catch(IOException ie) {
          System.err.println( ie );
        }
      }
    }
  }
}
