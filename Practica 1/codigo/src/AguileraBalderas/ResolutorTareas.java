package AguileraBalderas;

import java.util.ArrayList;

import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import AguileraBalderas.AEstrella;
import core.game.Observation;
import core.game.StateObservation;

public class ResolutorTareas {
	
	private ArrayList<Observation>[][] mundo;
	int ancho, alto;
	StateObservation obs;
	
	public ResolutorTareas(ArrayList<Observation>[][] mundo, int ancho, int alto, StateObservation obs) {
		this.mundo = mundo;
		this.ancho = ancho;
		this.alto = alto;
		this.obs = obs;
	}
	
	private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
	
	public ArrayList<Types.ACTIONS> obtenCamino(int col_actual,int fila_actual,int col_obj, int fila_obj, ElapsedCpuTimer timer){
		Nodo inicio = new Nodo(0, distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), col_actual, fila_actual, null, obs.getAvatarOrientation());
		Nodo fin = new Nodo(distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), 0, col_obj, fila_obj, null, obs.getAvatarOrientation());
		AEstrella aestrella = new AEstrella(inicio, fin, mundo);
		aestrella.buscaCamino(timer);
		return aestrella.devuelveAcciones(obs);
	}
	/* Reemplazable por la función ya definida en el código de prueba
	public ArrayList<Types.ACTIONS> salirPortal(int fila, int columna, ElapsedCpuTimer timer){
		//TODO: Usar obtenCamino
	}*/

}