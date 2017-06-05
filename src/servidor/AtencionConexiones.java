package servidor;

import com.google.gson.Gson;

import mensajeria.Comando;
import mensajeria.PaqueteDeUsuarios;

public class AtencionConexiones extends Thread {
	
	private final Gson gson = new Gson();

	public AtencionConexiones() {
	}

	public void run() {
		synchronized(this){
			try {
				while (true) {
					// Espero a que se conecte alguien
					wait();
					// Le reenvio la conexion a todos
					for (EscuchaCliente conectado : Servidor.getClientesConectados()) {
						if(conectado.getPaqueteUsuario().getEstado()){
							PaqueteDeUsuarios pdu = (PaqueteDeUsuarios) new PaqueteDeUsuarios(Servidor.getUsuariosConectados()).clone();
							pdu.setComando(Comando.CONEXION);
							System.out.println("ATENCIOPN CONEXIONES");
							conectado.getSalida().writeObject(gson.toJson(pdu));		
						}
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}