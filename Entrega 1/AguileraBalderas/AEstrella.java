package AguileraBalderas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AEstrella {
	//Nodo del que parte el camino de A*
	private Nodo nodo_inicial;
	//Nodo al que se quiere llegar con A*
	private Nodo nodo_objetivo;
	//Priority queue con abiertos que nos los ordena según la f
	private PriorityQueue<Nodo> abiertos;
	//Set con los abiertos para controlar de forma eficiente si un nodo está o no en abietos
	private Set<Nodo> abiertos_set;
	//Set con los cerrados para controlar de forma eficiente si un nodo está o no en cerrados
	private Set<Nodo> cerrados;
	//Grid con el mundo
	private ArrayList<Observation>[][] mundo;
	//Camino obtenido por buscaCamino tras ser ejecutado
	private List<Nodo> camino;
	// Tamaño del mundo
	int ancho, alto;
	// Mejor nodo en cada momento del A*
	Nodo mejor_nodo;
	// Set que contiene los contornos de los bichos, estas casillas no serán accesibles
	HashSet<Vector2di> contornos_bichos;
	
	/**
	 * Constructor de la clase AEstrella
	 * @param ancho Ancho del mundo
	 * @param alto Alto del mundo
	 */
	public AEstrella(int ancho, int alto) {
		this.contornos_bichos = new HashSet<Vector2di>();
		this.ancho = ancho;
		this.alto = alto;
		this.cerrados = new HashSet<Nodo>();
		this.abiertos_set = new HashSet<Nodo>();
		//La priority queue de abiertos ordena los nodos según su valor de f
		this.abiertos = new PriorityQueue<Nodo>(new Comparator<Nodo>() {
			@Override
			public int compare(Nodo nodo1, Nodo nodo2) {
				return Integer.compare(nodo1.f, nodo2.f);
			}
		});
	}
	
	/**
	 * Función que se emplea para actualizar los parámetros fundamentales de un objeto AEstrella. Esta función se utiliza para controlar la ejecución
	 * del algoritmo en varios turnos consecutivos.
	 * @param start Nodo desde donde se empieza
	 * @param end Nodo hasta donde queremos llegar
	 * @param mundo Matriz que representa el mundo
	 */
	public void setParametros(Nodo start, Nodo end, ArrayList<Observation>[][] mundo) {
		this.mundo = mundo;
		this.nodo_inicial = start;
		this.nodo_objetivo = end;
	}
	
	/**
	 * Función que calcula la g de un nodo, esto es el número de ticks que se han consumido para llegar a ese nodo teniendo en cuenta la orientación
	 * @param nodo Nodo del que queremos calcular el costo
	 * @return Devuelve un entero que representa el costo
	 */
	private int g(Nodo nodo) {
		//Cogemos la orientación del nodo y su padre
		Vector2d orientacion = nodo.padre.orientacion;
		Vector2d orientacion_nodo = nodo.orientacion;
		//Si la orientación del nodo y el padre no coinciden entonces se necesita un movimiento para cambiar la orientación y otro para moverse
		if(orientacion!=orientacion_nodo)
			return nodo.padre.coste_g+2;
		//Si la orientación del nodo y el padre coinciden entonces solo se necesita un movimiento para llegar del padre al nodo
		return nodo.padre.coste_g+1;
	}
	
	/**
	 * Función que calcula el costo estimado desde el nodo dado por fila y columna hasta el nodo objetivo
	 * @param columna Columna del nodo sobre el que se quiere estimar h
	 * @param fila Fila del nodo sobre el que se quiere estimar h
	 * @return Devuelve un entero que representa la distancia Manhattan entre el nodo actual y el objetivo
	 */
	private int h(int columna, int fila) {
		return Math.abs(fila - nodo_objetivo.fila) + Math.abs(columna - nodo_objetivo.columna);
	}
	
	/**
	 * Función que obtiene una lista con los vecinos de un nodo dado
	 * @param n Nodo del que queremos obtener los vecinos
	 * @return Devuelve un ArrayList de nodos representando a los vecinos
	 */
	public ArrayList<Nodo> obtenerVecinos(Nodo n) {
		ArrayList<Nodo> vecinos = new ArrayList<Nodo>();
		Vector2d orientacion = n.orientacion;
		if(n.columna-1>0)
			vecinos.add(new Nodo(-1, h(n.columna-1,n.fila), n.columna-1,n.fila, n, new Vector2d(-1,0)));
		if(n.columna+1<ancho)
			vecinos.add(new Nodo(-1, h(n.columna+1,n.fila), n.columna+1,n.fila, n, new Vector2d(1,0)));
		if(n.fila-1>0)
			vecinos.add(new Nodo(-1, h(n.columna,n.fila-1), n.columna,n.fila-1, n, new Vector2d(0,-1)));
		if(n.fila+1<alto)
			vecinos.add(new Nodo(-1, h(n.columna,n.fila+1), n.columna,n.fila+1, n, new Vector2d(0,1)));
		return vecinos;
	}

	/**
	 * Implementa la distancia manhattan
	 * @param n1 Uno de los extremos del camino
	 * @param n2 El otro extremo
	 * @return Devuelve un entero que representa la distancia manhattan entre los nodos n1 y n2
	 */
	public int distanciaManhattan(Nodo n1, Nodo n2) {
		return Math.abs(n1.fila-n2.fila) + Math.abs(n1.columna-n2.columna);
	}
	
	/**
	 * Funcion que implementa el algoritmo A* para obtener el camino entre el nodo inicial y el objetivo
	 * @param timer Objeto de tipo {@link ElapsedCpuTimer} para controlar el tiempo transcurrido
	 * @param notime Booleano que nos indica si queremos que se tenga en cuenta o no el tiempo en el algoritmo.
	 * @return
	 */
	public List<Nodo> buscaCamino(ElapsedCpuTimer timer, boolean notime){
		//Inicializamos el camino, los abiertos y el mejor nodo.
		List<Nodo> path = new ArrayList<Nodo>();
		mejor_nodo = nodo_inicial;
		if(!abiertos_set.contains(nodo_inicial)) {
			abiertos.add(nodo_inicial);
			abiertos_set.add(nodo_inicial);
		}
		// Mientras que queden abiertos y no hayamos excedido el tiempo
		while(!isEmpty(abiertos) && (timer.elapsedMillis() < 25 || notime)) {
			// Sacamos el siguiente nodo de abiertos y actualizamos el mejor nodo
			Nodo nodo_actual = abiertos.poll();
			if(nodo_actual.f<mejor_nodo.f)
				mejor_nodo = nodo_actual;
			abiertos_set.remove(nodo_actual);
			// Si el nodo sacado es el objetivo
			if(nodo_actual.equals(nodo_objetivo)) {
				// Reconstruimos el camino y lo devolvemos
				while(!nodo_actual.equals(nodo_inicial)) {
					path.add(nodo_actual);
					nodo_actual = nodo_actual.padre;
				}
				path.add(nodo_actual);
				Collections.reverse(path);
				camino = path;
				return path;
			}
			// Obtenemos los vecinos
			List<Nodo> vecinos = obtenerVecinos(nodo_actual);
			// Para cada vecino
			for(int i=0; i < vecinos.size(); i++) {
				//Comprobamos si es accesible
				boolean accesible = isAccesible(mundo,vecinos.get(i));
				if(accesible) {
					// Actualizamos su padre, el coste y la f.
					vecinos.get(i).padre = nodo_actual;
					vecinos.get(i).coste_g = g(vecinos.get(i));
					vecinos.get(i).f = vecinos.get(i).coste_g + vecinos.get(i).estimacion_h;
					// Si ya esta en cerrados no hacemos nada
					if(cerrados.contains(vecinos.get(i)))
						continue;
					// Si esta en abiertos actualizamos para que tenga al mejor padre y no lo metemos en abiertos de nuevo
					if(abiertos_set.contains(vecinos.get(i))) {
						java.util.Iterator<Nodo> it2 = abiertos.iterator();
						while(it2.hasNext()) {
					        Nodo nodo = it2.next();
					        if (nodo.equals(vecinos.get(i))&&nodo_actual.f<nodo.f)
					        	nodo.padre=nodo_actual;
					    }
						continue;
					}
					// En caso de que no esté ni en abiertos ni en cerrados lo añadimos
					abiertos.add(vecinos.get(i));
					abiertos_set.add(vecinos.get(i));
				}
			}
			// Metemos en cerrados el nodo actual
			cerrados.add(nodo_actual);
		}
		// Si no hemos encontrado el camino hasta el nodo objetivo reconstruimos el camino hasta el mejor nodo que hemos sacado
		while(!mejor_nodo.equals(nodo_inicial)) {
			path.add(mejor_nodo);
			mejor_nodo = mejor_nodo.padre;
		}
		path.add(mejor_nodo);
		Collections.reverse(path);
		camino = path;
		return path;
	}		
	
	/**
	 * Función que comprueba si un nodo es accesible
	 * @param mundo Grid del mundo
	 * @param nodo Nodo que queremos comprobar si es accesible
	 * @return Devuelve un booleano indicando si el nodo es accesible
	 */
	private boolean isAccesible(ArrayList<Observation>[][] mundo, Nodo nodo) {
		int fila = nodo.fila;
		int columna = nodo.columna;
		// Si el nodo está vacío es accesible
		boolean vacio = mundo[columna][fila].size()==0;
		if(!vacio) {
			//Esta en el contorno de un bicho
			boolean contorno_bicho = contornos_bichos.contains(new Vector2di(columna, fila));
			// Comprueba si hay un bicho
			boolean bicho = mundo[columna][fila].get(0).itype==11 || mundo[columna][fila].get(0).itype==10;
			// Comprueba si hay un muro
			boolean muro = mundo[columna][fila].get(0).itype==0;
			// Comprueba si hay una piedra
			boolean piedra = mundo[columna][fila].get(0).itype==7;
			// Comprueba si la casilla tiene una piedra encima
			boolean piedra_arriba = false;
			if(fila>0 && mundo[columna][fila-1].size()>0)
				piedra_arriba = mundo[columna][fila-1].get(0).itype==7;
			// Si no hay un bicho ni un muro ni una piedra ni una piedra encima ni está en el contorno de un bicho entonces es una casilla accesible
			boolean condicion;
			condicion = !bicho && !muro && !piedra && !piedra_arriba && !contorno_bicho;
			return condicion;
		}
		return true;
	}
	
	/*
	 * Función que comprueba si una priority queue de nodos está vacía
	 */
	private boolean isEmpty(PriorityQueue<Nodo> openList) {
        return openList.size() == 0;
	}
	
	/*
	 * Función que una vez calculado el camino por el A* devuelve la lista de acciones que te llevan por dicho camino
	 * @param obs Objeto de tipo StateObservation que define el estado actual del mundo y el avatar.
	 */
	public ArrayList<Types.ACTIONS> devuelveAcciones(StateObservation obs){
		Nodo nodo_actual = nodo_inicial;
		ArrayList<Types.ACTIONS> acciones = new ArrayList<Types.ACTIONS>();
		
		// En función de la orientación hay que dar uno o dos movimientos
		if(camino.size()>1) {
			if(camino.get(1).fila > nodo_actual.fila)
				if(obs.getAvatarOrientation().y==1.0)
					acciones.add(Types.ACTIONS.ACTION_DOWN);
				else {
					acciones.add(Types.ACTIONS.ACTION_DOWN);
					acciones.add(Types.ACTIONS.ACTION_DOWN);
				}
			
			else if(camino.get(1).fila < nodo_actual.fila)
				if(obs.getAvatarOrientation().y==-1.0)
					acciones.add(Types.ACTIONS.ACTION_UP);
				else {
					acciones.add(Types.ACTIONS.ACTION_UP);
					acciones.add(Types.ACTIONS.ACTION_UP);
				}
			
			else if(camino.get(1).columna > nodo_actual.columna)
				if(obs.getAvatarOrientation().x==1.0)
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
				else {
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
				}
			
			else if(camino.get(1).columna < nodo_actual.columna)
				if(obs.getAvatarOrientation().x==-1.0)
					acciones.add(Types.ACTIONS.ACTION_LEFT);
				else {
					acciones.add(Types.ACTIONS.ACTION_LEFT);
					acciones.add(Types.ACTIONS.ACTION_LEFT);
				}
			
			else
				acciones.add(Types.ACTIONS.ACTION_NIL);
			nodo_actual = camino.get(1);
		}
		
		// Para los siguientes pasos hay que comprobar si la última acción fue la misma o no
		for(int i=2; i < camino.size(); i++) {
			Vector2d orientacion = obs.getAvatarOrientation();
			//System.out.printf("Nodo actual: Fila %d, Columna %d\n",nodo_actual.fila, nodo_actual.columna);
			//System.out.printf("Nodo del camino: Fila %d, Columna %d\n\n",camino.get(i).fila, camino.get(i).columna);
			if(camino.get(i).fila > nodo_actual.fila)
				if(acciones.get(acciones.size()-1)==Types.ACTIONS.ACTION_DOWN)
					acciones.add(Types.ACTIONS.ACTION_DOWN);
				else {
					acciones.add(Types.ACTIONS.ACTION_DOWN);
					acciones.add(Types.ACTIONS.ACTION_DOWN);
				}
			
			else if(camino.get(i).fila < nodo_actual.fila)
				if(acciones.get(acciones.size()-1)==Types.ACTIONS.ACTION_UP)
					acciones.add(Types.ACTIONS.ACTION_UP);
				else {
					acciones.add(Types.ACTIONS.ACTION_UP);
					acciones.add(Types.ACTIONS.ACTION_UP);
				}
			
			else if(camino.get(i).columna > nodo_actual.columna)
				if(acciones.get(acciones.size()-1)==Types.ACTIONS.ACTION_RIGHT)
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
				else {
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
					acciones.add(Types.ACTIONS.ACTION_RIGHT);
				}
			
			else if(camino.get(i).columna < nodo_actual.columna)
				if(acciones.get(acciones.size()-1)==Types.ACTIONS.ACTION_LEFT)
					acciones.add(Types.ACTIONS.ACTION_LEFT);
				else {
					acciones.add(Types.ACTIONS.ACTION_LEFT);
					acciones.add(Types.ACTIONS.ACTION_LEFT);
				}
			
			else
				acciones.add(Types.ACTIONS.ACTION_NIL);
			nodo_actual = camino.get(i);
		}
		return acciones;
	}
	
	/**
	 * Función que resetea el estado actual de un objeto de tipo AEstrella. Esta función se usa para controlar la ejecución del algoritmo
	 * en varios turnos consecutivos con la misma información.
	 */
	public void reset() {
		this.cerrados = new HashSet<Nodo>();
		this.abiertos_set = new HashSet<Nodo>();
		//La priority queue de abiertos ordena los nodos según su valor de f
		this.abiertos = new PriorityQueue<Nodo>(new Comparator<Nodo>() {
			@Override
			public int compare(Nodo nodo1, Nodo nodo2) {
				return Integer.compare(nodo1.f, nodo2.f);
			}
		});
	}
}
