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

    private DataOutputStream out = null;
     private DataInputStream in = null;
     private Socket s;
    // static private final Charset charset = Charset.forName("UTF8");
    //static private final CharsetDecoder decoder = charset.newDecoder();
    //static private final CharsetEncoder enc = Charset.forName("US-ASCII").newEncoder();

    
    // Método a usar para acrescentar uma string à caixa de texto
    // * NÃO MODIFICAR *
    public void printMessage(final String message) {
        chatArea.append(message);
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
                chatBox.requestFocus();
            }
        });
        // --- Fim da inicialização da interface gráfica

        // Se for necessário adicionar código de inicialização ao
        // construtor, deve ser colocado aqui

        try {
            s = new Socket(server, port);
        }
        catch(IOException i) {
        }

        //private DataInputStream input = null;

    }


    // Método invocado sempre que o utilizador insere uma mensagem
    // na caixa de entrada
    public void newMessage(String message) throws IOException {
        PrintWriter pw = new PrintWriter(s.getOutputStream(), true);
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        pw.println(message);
        String response = br.readLine();
        printMessage(response);
        printMessage("\n");
    }

    
    // Método principal do objecto
    public void run() throws IOException {
        // PREENCHER AQUI
        BufferedReader br = new BufferedReader(new InputStreamReader(s.getInputStream()));
        String response;
        while (true) {
            if (br.ready()) {
                response = br.readLine();
                printMessage(response);
                printMessage("\n");
            }
        }
    }
    

    // Instancia o ChatClient e arranca-o invocando o seu método run()
    // * NÃO MODIFICAR *
    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient(args[0], Integer.parseInt(args[1]));
        client.run();
    }

}