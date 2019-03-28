package AguileraBalderas;

import java.util.ArrayList;

import ontology.Types;
import ontology.Types.ACTIONS;
import AguileraBalderas.AEstrella;

public class ResolutorTareas {
	
	public ResolutorTareas() {
		
	}
	
	private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
	
	public ArrayList<Types.ACTIONS> obtenCamino(int fila_actual,int col_actual,int fila_obj, int col_obj, ArrayList<ArrayList<Character> > mundo){
		Nodo inicio = new Nodo(0, distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), fila_actual, col_actual, null, false);
		Nodo fin = new Nodo(distanciaManhattan(fila_actual, col_actual, fila_obj, col_obj), 0, fila_obj, col_obj, null, false);
		AEstrella aestrella = new AEstrella(inicio, fin, mundo);;
		return aestrella.devuelveAcciones();
	}
	
	

}
