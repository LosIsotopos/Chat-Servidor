package servidor;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;

import com.google.gson.Gson;

import mensajeria.Comando;
import mensajeria.Paquete;
import mensajeria.PaqueteDeUsuarios;
import mensajeria.PaqueteUsuario;

public class EscuchaCliente extends Thread {

	private final Socket socket;
	private final ObjectInputStream entrada;
	private final ObjectOutputStream salida;
//	private int idEmisor;
	private final Gson gson = new Gson();
	
	private PaqueteUsuario paqueteUsuario;
	private PaqueteDeUsuarios paqueteDeUsuarios;

	public EscuchaCliente(String ip, Socket socket, ObjectInputStream entrada, ObjectOutputStream salida) {
		this.socket = socket;
		this.entrada = entrada;
		this.salida = salida;
		paqueteUsuario = new PaqueteUsuario();
	}

	public void run() {
		try {

			Paquete paquete;
			Paquete paqueteSv = new Paquete(null, 0);
			PaqueteUsuario paqueteUsuario = new PaqueteUsuario();

			String cadenaLeida = (String) entrada.readObject();
		
			while (!((paquete = gson.fromJson(cadenaLeida, Paquete.class)).getComando() == Comando.DESCONECTAR)){
								
				switch (paquete.getComando()) {
					
					case Comando.CONEXION:
						paqueteUsuario = (PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class)).clone();
						
						Servidor.getPersonajesConectados().put(paqueteUsuario.getUsername(), (PaqueteUsuario) paqueteUsuario.clone());
						Servidor.getUsuariosConectados().add(paqueteUsuario.getUsername());
						
						synchronized(Servidor.atencionConexiones){
							Servidor.atencionConexiones.notify();
						}
						
						break;
						
					case Comando.INICIOSESION:
						paqueteSv.setComando(Comando.INICIOSESION);
						
						// Recibo el paquete usuario
						paqueteUsuario = (PaqueteUsuario) (gson.fromJson(cadenaLeida, PaqueteUsuario.class));
						
						// Si se puede loguear el usuario le envio un mensaje de exito y el paquete personaje con los datos
						if (Servidor.loguearUsuario(paqueteUsuario)) {
							
							paqueteUsuario = new PaqueteUsuario();
							paqueteUsuario.setComando(Comando.INICIOSESION);
							paqueteUsuario.setMensaje(Paquete.msjExito);
							
							salida.writeObject(gson.toJson(paqueteUsuario));
							
						} else {
							paqueteSv.setMensaje(Paquete.msjFracaso);
							salida.writeObject(gson.toJson(paqueteSv));
						}
						break;
						
					case Comando.TALK:
						break;
						
					case Comando.SALIR:
						
						// Cierro todo
						entrada.close();
						salida.close();
						socket.close();
						
						// Lo elimino de los clientes conectados
						Servidor.getClientesConectados().remove(this);
						
						// Indico que se desconecto
						Servidor.log.append(paquete.getIp() + " se ha desconectado." + System.lineSeparator());
						
						return;
						
					case Comando.DESCONECTAR:
						Servidor.log.append(paqueteUsuario.getUsername() + " se ha desconectado." + System.lineSeparator());
						break;
//					case Comando.BATALLA:
//						
//						// Le reenvio al id del personaje batallado que quieren pelear
//						paqueteBatalla = (PaqueteBatalla) gson.fromJson(cadenaLeida, PaqueteBatalla.class);
//						Servidor.log.append(paqueteBatalla.getId() + " quiere batallar con " + paqueteBatalla.getIdEnemigo() + System.lineSeparator());
//						
//						//seteo estado de batalla
//						Servidor.getPersonajesConectados().get(paqueteBatalla.getId()).setEstado(Estado.estadoBatalla);
//						Servidor.getPersonajesConectados().get(paqueteBatalla.getIdEnemigo()).setEstado(Estado.estadoBatalla);
//						paqueteBatalla.setMiTurno(true);
//						salida.writeObject(gson.toJson(paqueteBatalla));
//						for(EscuchaCliente conectado : Servidor.getClientesConectados()){
//							if(conectado.getIdPersonaje() == paqueteBatalla.getIdEnemigo()){
//								int aux = paqueteBatalla.getId();
//								paqueteBatalla.setId(paqueteBatalla.getIdEnemigo());
//								paqueteBatalla.setIdEnemigo(aux);
//								paqueteBatalla.setMiTurno(false);
//								conectado.getSalida().writeObject(gson.toJson(paqueteBatalla));
//								break;
//							}
//						}
//						
//						synchronized(Servidor.atencionConexiones){
//							Servidor.atencionConexiones.notify();
//						}
//						
//						break;
//						
//					case Comando.ATACAR: 
//						paqueteAtacar = (PaqueteAtacar) gson.fromJson(cadenaLeida, PaqueteAtacar.class);
//						for(EscuchaCliente conectado : Servidor.getClientesConectados()) {
//							if(conectado.getIdPersonaje() == paqueteAtacar.getIdEnemigo()) {
//								conectado.getSalida().writeObject(gson.toJson(paqueteAtacar));
//							}
//						}
//						break;
					default:
						break;
					}
				
				cadenaLeida = (String) entrada.readObject();
			}

			entrada.close();
			salida.close();
			socket.close();

			Servidor.getPersonajesConectados().remove(paqueteUsuario.getUsername());
			Servidor.getUsuariosConectados().remove(paqueteUsuario.getUsername());
			Servidor.getClientesConectados().remove(this);

			for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
				paqueteDeUsuarios = new PaqueteDeUsuarios(Servidor.getUsuariosConectados());
				paqueteDeUsuarios.setComando(Comando.CONEXION);
				conectado.salida.writeObject(gson.toJson(paqueteDeUsuarios, PaqueteDeUsuarios.class));
			}

			Servidor.log.append(paquete.getIp() + " se ha desconectado." + System.lineSeparator());

		} catch (IOException | ClassNotFoundException e) {
			Servidor.log.append("Error de conexion: " + e.getMessage() + System.lineSeparator());
			e.printStackTrace();
		} 
	}
	
	public Socket getSocket() {
		return socket;
	}
	
	public ObjectInputStream getEntrada() {
		return entrada;
	}
	
	public ObjectOutputStream getSalida() {
		return salida;
	}
	
//	public int getIdEmisor() {
//		return idEmisor;
//	}

	public PaqueteUsuario getPaqueteUsuario() {
		return paqueteUsuario;
	}

	public void setPaqueteUsuario(PaqueteUsuario paqueteUsuario) {
		this.paqueteUsuario = paqueteUsuario;
	}


}

