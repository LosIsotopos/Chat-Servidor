package servidor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;

import mensajeria.PaqueteMensaje;
import mensajeria.PaqueteUsuario;
import java.awt.TextArea;

public class Servidor extends Thread {
	public static ArrayList<Socket> SocketsConectados = new ArrayList<Socket>();
	public static ArrayList<String> UsuariosConectados = new ArrayList<String>();
	private static ArrayList<EscuchaCliente> clientesConectados = new ArrayList<>();
	public static Map<String, Socket> mapConectados = new HashMap<>();
	
	private static ServerSocket serverSocket;
	private final int puerto = 9999;
	
	private static Thread server;
	
	static TextArea log = new TextArea();
	static boolean estadoServer;
	
	public static AtencionConexiones atencionConexiones;
	
	public static void main(String[] args) {
		cargarInterfaz();
	}
	
	private static void cargarInterfaz() {
		JFrame ventana = new JFrame("Servidor del Chat");
		ventana.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		ventana.setSize(542, 538);
		ventana.setResizable(false);
		ventana.setLocationRelativeTo(null);
		ventana.getContentPane().setLayout(null);

		JScrollPane scrollPane = new JScrollPane();
		scrollPane.setBounds(12, 13, 512, 434);
		ventana.getContentPane().add(scrollPane);
		log.setEditable(false);
		
		scrollPane.setViewportView(log);
		
		final JButton botonIniciar = new JButton();
		final JButton botonDetener = new JButton();
		botonIniciar.setText("Start");
		botonIniciar.setBounds(12, 460, 100, 30);
		botonIniciar.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				server = new Thread(new Servidor());
				server.start();
				botonIniciar.setEnabled(false);
				botonDetener.setEnabled(true);
			}
		});

		ventana.getContentPane().add(botonIniciar);

		botonDetener.setText("Stop");
		botonDetener.setBounds(424, 460, 100, 30);
		botonDetener.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					estadoServer = false;
					UsuariosConectados = new ArrayList<String>();
					server.stop();
					atencionConexiones.stop();
					for (EscuchaCliente cliente : clientesConectados) {
						cliente.getSalida().close();
						cliente.getEntrada().close();
						cliente.getSocket().close();
					}
					serverSocket.close();
					log.append("El servidor se ha detenido." + System.lineSeparator());
				} catch (IOException e1) {
					log.append("Fallo al intentar detener el servidor." + System.lineSeparator());
					e1.printStackTrace();
				}
				botonDetener.setEnabled(false);
				botonIniciar.setEnabled(true);
			}
		});
		botonDetener.setEnabled(false);
		ventana.getContentPane().add(botonDetener);
		
		
		

		ventana.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
		ventana.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent evt) {
				if (serverSocket != null) {
					try {
						estadoServer = false;
						UsuariosConectados = new ArrayList<String>();
						server.stop();
						atencionConexiones.stop();
						for (EscuchaCliente cliente : clientesConectados) {
							cliente.getSalida().close();
							cliente.getEntrada().close();
							cliente.getSocket().close();
						}
						serverSocket.close();
					} catch (IOException e) {
						log.append("Fallo al intentar detener el servidor." + System.lineSeparator());
						e.printStackTrace();
						System.exit(1);
					}
				}
				System.exit(0);
			}
		});

		ventana.setVisible(true);
	}
	
	@Override
	public void run() {
		try {
			estadoServer = true;
			log.append("Iniciando el servidor..." + System.lineSeparator());
			serverSocket = new ServerSocket(puerto);
			log.append("Servidor esperando conexiones..." + System.lineSeparator());
			String ipRemota;
			
			atencionConexiones = new AtencionConexiones();
			atencionConexiones.start();
		
			while (estadoServer) {
				Socket cliente = serverSocket.accept();
				//Agrego el Socket a la lista de Sockets
				SocketsConectados.add(cliente);
				
				ipRemota = cliente.getInetAddress().getHostAddress();
				log.append(ipRemota + " se ha conectado" + System.lineSeparator());

				ObjectOutputStream salida = new ObjectOutputStream(cliente.getOutputStream());
				ObjectInputStream entrada = new ObjectInputStream(cliente.getInputStream());
				
				EscuchaCliente atencion = new EscuchaCliente(ipRemota, cliente, entrada, salida);
				atencion.start();
				clientesConectados.add(atencion);
			}
		} catch (Exception e) {
			log.append("Fallo la conexión." + System.lineSeparator());
			e.printStackTrace();
		}
	}
	
	public static ArrayList<EscuchaCliente> getClientesConectados() {
		return clientesConectados;
	}

	public static void setClientesConectados(ArrayList<EscuchaCliente> clientesConectados) {
		Servidor.clientesConectados = clientesConectados;
	}

	public static ArrayList<String> getUsuariosConectados() {
		return UsuariosConectados;
	}
	
	public static ArrayList<Socket> getSocketsConectados() {
		return SocketsConectados;
	}

	public static void setSocketsConectados(ArrayList<Socket> socketsConectados) {
		SocketsConectados = socketsConectados;
	}

	public static boolean loguearUsuario(PaqueteUsuario user) {
		boolean result = true;
		if(UsuariosConectados.contains(user.getUsername())) {
			result = false;
		}
		// Si existe inicio sesion
		if (result) {
			Servidor.log.append("El usuario " + user.getUsername() + " ha iniciado sesión." + System.lineSeparator());
			return true;
		} else {
			// Si no existe informo y devuelvo false
			Servidor.log.append("El usuario " + user.getUsername() + " ya se encuentra logeado." + System.lineSeparator());
			return false;
		}
	}

	public static boolean mensajeAUsuario(PaqueteMensaje pqm) {
		boolean result = true;
		if(!UsuariosConectados.contains(pqm.getUserReceptor())) {
			result = false;
		}
		// Si existe inicio sesion
		if (result) {
			Servidor.log.append(pqm.getUserEmisor() + " envió mensaje a " + pqm.getUserReceptor() + System.lineSeparator());
				return true;
		} else {
			// Si no existe informo y devuelvo false
			Servidor.log.append("El mensaje para " + pqm.getUserReceptor() + " no se envió, ya que se encuentra desconectado." + System.lineSeparator());
			return false;
		}
	}
	
	public static boolean mensajeAAll(int contador) {
		boolean result = true;
		if(UsuariosConectados.size() != contador+1) {
			result = false;
		}
		// Si existe inicio sesion
		if (result) {
			Servidor.log.append("Se ha enviado un mensaje a todos los usuarios" + System.lineSeparator());
				return true;
		} else {
			// Si no existe informo y devuelvo false
			Servidor.log.append("Uno o más de todos los usuarios se ha desconectado, se ha mandado el mensaje a los demas." + System.lineSeparator());
			return false;
		}
	}
	
	public static Map<String, Socket> getPersonajesConectados() {
		return mapConectados;
	}

	public static void setPersonajesConectados(Map<String, Socket> personajesConectados) {
		Servidor.mapConectados = personajesConectados;
	}
}