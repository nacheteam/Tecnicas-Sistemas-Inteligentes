package AguileraBalderas;

import java.util.ArrayList;

import ontology.Types;
import ontology.Types.ACTIONS;
import AguileraBalderas.AEstrella;

public class ResolutorTareas {
	
	private ArrayList<ArrayList<Character> > mundo;
	
	public ResolutorTareas(ArrayList<ArrayList<Character> > mundo) {
		this.mundo = mundo;
	}
	
	private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
	
	public ArrayList<Types.ACTIONS> obtenCamino(int fila_actual,int col_actual,int fila_obj, int col_obj){
		Nodo inicio = new Nodo(0, distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), fila_actual, col_actual, null, false);
		Nodo fin = new Nodo(distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), 0, fila_obj, col_obj, null, false);
		AEstrella aestrella = new AEstrella(inicio, fin, mundo);;
		return aestrella.devuelveAcciones();
	}
	
	public ArrayList<Types.ACTIONS> salirPortal(int fila, int columna){
		int fila_portal = 0;
		int columna_portal = 0;
		for(int i = 0; i < mundo.size(); ++i) {
			for(int j = 0; j < mundo.get(i).size(); ++j) {
				if(mundo.get(i).get(j)=='p') {
					fila_portal = i;
					columna_portal = j;
				}
			}
		}
		ArrayList<Types.ACTIONS> acciones = obtenCamino(fila, columna, fila_portal, columna_portal);
		acciones.add(Types.ACTIONS.ACTION_ESCAPE);
		return(acciones);
	}

}
