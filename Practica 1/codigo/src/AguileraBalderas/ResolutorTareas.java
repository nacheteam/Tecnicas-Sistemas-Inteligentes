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
	public ArrayList<Types.ACTIONS> salirPortal(ElapsedCpuTimer timer, double fescalaX, double fescalaY){
		int col_portal = (int) Math.round(this.obs.getPortalsPositions()[0].get(0).position.x / fescalaX);
    	int fila_portal = (int) Math.round(this.obs.getPortalsPositions()[0].get(0).position.y / fescalaY);
    	
    	int col_avatar = (int) Math.round(this.obs.getAvatarPosition().x / fescalaX);
    	int fila_avatar = (int) Math.round(this.obs.getAvatarPosition().y / fescalaY);
    	
    	ArrayList<Types.ACTIONS> acciones = new ArrayList<Types.ACTIONS>();
    	
    	if(col_portal==col_avatar && fila_portal==fila_avatar) {
    		acciones.add(Types.ACTIONS.ACTION_NIL);
    		return acciones;
    	}
    	return obtenCamino(col_avatar, fila_avatar, col_portal, fila_portal, timer);
    	
	}

}