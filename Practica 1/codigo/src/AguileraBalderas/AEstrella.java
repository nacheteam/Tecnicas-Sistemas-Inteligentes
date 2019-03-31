package AguileraBalderas;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import javax.swing.text.html.HTMLDocument.Iterator;

import core.game.Observation;
import core.game.StateObservation;
import ontology.Types;
import tools.ElapsedCpuTimer;
import tools.Vector2d;

public class AEstrella {
	private Nodo nodo_inicial;
	private Nodo nodo_objetivo;
	private PriorityQueue<Nodo> abiertos;
	private Set<Nodo> abiertos_set;
	private Set<Nodo> cerrados;
	private ArrayList<Observation>[][] mundo;
	private List<Nodo> camino;
	
	public AEstrella(Nodo start, Nodo end,ArrayList<Observation>[][] mundo) {
		this.nodo_inicial = start;
		this.nodo_objetivo = end;
		this.mundo = mundo;
		this.cerrados = new HashSet<Nodo>();
		this.abiertos_set = new HashSet<Nodo>();
		this.abiertos = new PriorityQueue<Nodo>(new Comparator<Nodo>() {
			@Override
			public int compare(Nodo nodo1, Nodo nodo2) {
				return Integer.compare(nodo1.f, nodo2.f);
			}
		});
	}
	
	private int g(Nodo nodo) {
		Vector2d orientacion = nodo.padre.orientacion;
		Vector2d orientacion_nodo = nodo.orientacion;
		if(orientacion!=orientacion_nodo)
			return nodo.padre.coste_g+2;
		return nodo.padre.coste_g+1;
	}
	
	private int h(int columna, int fila) {
		return Math.abs(fila - nodo_objetivo.fila) + Math.abs(columna - nodo_objetivo.columna);
	}
	
	public ArrayList<Nodo> obtenerVecinos(Nodo n) {
		ArrayList<Nodo> vecinos = new ArrayList<Nodo>();
		Vector2d orientacion = n.orientacion;
		vecinos.add(new Nodo(-1, h(n.columna-1,n.fila), n.columna-1,n.fila, n, new Vector2d(-1,0)));
		vecinos.add(new Nodo(-1, h(n.columna+1,n.fila), n.columna+1,n.fila, n, new Vector2d(1,0)));
		vecinos.add(new Nodo(-1, h(n.columna,n.fila-1), n.columna,n.fila-1, n, new Vector2d(0,-1)));
		vecinos.add(new Nodo(-1, h(n.columna,n.fila+1), n.columna,n.fila+1, n, new Vector2d(0,1)));
		return vecinos;
	}

	public int distanciaManhattan(Nodo n1, Nodo n2) {
		return Math.abs(n1.fila-n2.fila) + Math.abs(n1.columna-n2.columna);
	}
	
	public List<Nodo> buscaCamino(ElapsedCpuTimer timer){
		List<Nodo> path = new ArrayList<Nodo>();
		abiertos.add(nodo_inicial);
		abiertos_set.add(nodo_inicial);
		Nodo mejor_nodo = nodo_inicial;
		while(!isEmpty(abiertos) && timer.elapsedMillis() < 35) {
			long tiempo = timer.elapsedMillis();
			Nodo nodo_actual = abiertos.poll();
			mejor_nodo = nodo_actual;
			abiertos_set.remove(nodo_actual);
			if(nodo_actual.equals(nodo_objetivo)) {
				while(!nodo_actual.equals(nodo_inicial)) {
					path.add(nodo_actual);
					nodo_actual = nodo_actual.padre;
				}
				path.add(nodo_actual);
				Collections.reverse(path);
				camino = path;
				return path;
			}
			List<Nodo> vecinos = obtenerVecinos(nodo_actual);
			for(int i=0; i < vecinos.size(); i++) {
				boolean accesible = isAccesible(mundo,vecinos.get(i));
				if(accesible) {
					vecinos.get(i).padre = nodo_actual;
					vecinos.get(i).coste_g = g(vecinos.get(i));
					vecinos.get(i).f = vecinos.get(i).coste_g + vecinos.get(i).estimacion_h;
					if(cerrados.contains(vecinos.get(i)))
						continue;
					if(abiertos_set.contains(vecinos.get(i))) {
						java.util.Iterator<Nodo> it = abiertos_set.iterator();
						while(it.hasNext()) {
					        Nodo nodo = it.next();
					        if (nodo.equals(vecinos.get(i))&&nodo_actual.f<nodo.f)
					        	nodo.padre=nodo_actual;
					    }
						java.util.Iterator<Nodo> it2 = abiertos.iterator();
						while(it2.hasNext()) {
					        Nodo nodo = it2.next();
					        if (nodo.equals(vecinos.get(i))&&nodo_actual.f<nodo.f)
					        	nodo.padre=nodo_actual;
					    }
						continue;
					}
						
					abiertos.add(vecinos.get(i));
					abiertos_set.add(vecinos.get(i));
				}
			}
			cerrados.add(nodo_actual);
		}
		while(!mejor_nodo.equals(nodo_inicial)) {
			path.add(mejor_nodo);
			mejor_nodo = mejor_nodo.padre;
		}
		path.add(mejor_nodo);
		Collections.reverse(path);
		camino = path;
		return path;
	}
	
	private boolean isAccesible(ArrayList<Observation>[][] mundo, Nodo nodo) {
		int fila = nodo.fila;
		int columna = nodo.columna;
		boolean vacio = mundo[columna][fila].size()==0;
		if(!vacio) {
			boolean bicho = mundo[columna][fila].get(0).itype==11 || mundo[columna][fila].get(0).itype==10;
			boolean muro = mundo[columna][fila].get(0).itype==0;
			boolean piedra = mundo[columna][fila].get(0).itype==7;
			boolean piedra_arriba = false;
			if(fila>0 && mundo[columna][fila-1].size()>0)
				piedra_arriba = mundo[columna][fila-1].get(0).itype==7;
			boolean condicion = !bicho && !muro && !piedra && !piedra_arriba;
			return condicion;
		}
		return true;
	}

	private boolean isEmpty(PriorityQueue<Nodo> openList) {
        return openList.size() == 0;
	}
	
	public ArrayList<Types.ACTIONS> devuelveAcciones(StateObservation obs){
		Nodo nodo_actual = nodo_inicial;
		//System.out.println(this.camino.toString());
		ArrayList<Types.ACTIONS> acciones = new ArrayList<Types.ACTIONS>();
		
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
}