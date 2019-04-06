/*package AguileraBalderas;

import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import tools.ElapsedCpuTimer;
import java.util.Random;

public class Agent extends AbstractPlayer{

	public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
	}
	
	public void init(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
		
	}
	
	public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		Random aleatorio = new Random(System.currentTimeMillis());
		while(true) {
			int intAleatorio = aleatorio.nextInt(4);
			switch (intAleatorio) {
			case 0:
				return Types.ACTIONS.ACTION_LEFT;
			case 1:
				return Types.ACTIONS.ACTION_RIGHT;
			case 2:
				return Types.ACTIONS.ACTION_UP;
			case 3:
				return Types.ACTIONS.ACTION_DOWN;
			default:
				break;
			}
		}
	}
	
}*/
package AguileraBalderas;

import core.game.Observation;
import core.game.StateObservation;
import core.player.AbstractPlayer;
import ontology.Types;
import ontology.Types.ACTIONS;
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.Node;
import tools.pathfinder.PathFinder;

import AguileraBalderas.ResolutorTareas;

import javax.swing.*;

import java.util.ArrayList;
import java.util.HashMap;

public class Agent extends AbstractPlayer {
    //Objeto de clase Pathfinder
    private PathFinder pf;
    private int fescalaX;
    private int fescalaY;
    
    private ArrayList<Types.ACTIONS> lista_acciones;
    private ArrayList<Vector2di> lista_gemas_faciles;
    
    private ResolutorTareas resolutor;
    
    private boolean acabado;
    
    int alto, ancho;
    
        
    
    private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
    
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {  
    	acabado = false;
 	
    	lista_acciones = new ArrayList<Types.ACTIONS>();
        //Creamos una lista de IDs de obstaculos
        ArrayList<Integer> tiposObs = new ArrayList();
        tiposObs.add(0); //<- Muros
        tiposObs.add(7); //<- Piedras

        //Se inicializa el objeto del pathfinder con las ids de los obstaculos
        pf = new PathFinder(tiposObs);
        pf.VERBOSE = false; // <- Activa o desactiva el modo la impresión del log

        //Se lanza el algoritmo de pathfinding para poder ser usado en la función ACT
        pf.run(stateObs);

        this.fescalaX = stateObs.getWorldDimension().width / stateObs.getObservationGrid().length;
        this.fescalaY = stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length;
        
        lista_gemas_faciles = new ArrayList<Vector2di>();
        /*lista_gemas_faciles.add(new Vector2di(1,4));
        lista_gemas_faciles.add(new Vector2di(7,9));
        lista_gemas_faciles.add(new Vector2di(9,10));
        lista_gemas_faciles.add(new Vector2di(16,9));
        lista_gemas_faciles.add(new Vector2di(18,9));
        lista_gemas_faciles.add(new Vector2di(20,3));
        lista_gemas_faciles.add(new Vector2di(23,6));
        lista_gemas_faciles.add(new Vector2di(23,7));
        lista_gemas_faciles.add(new Vector2di(24,6));
        lista_gemas_faciles.add(new Vector2di(24,7));*/
        
        lista_gemas_faciles = obtenListaGemasFaciles(stateObs,elapsedTimer);
        System.out.println(lista_gemas_faciles);
        
        
        resolutor = new ResolutorTareas(stateObs.getObservationGrid(), stateObs.getWorldDimension().width, stateObs.getWorldDimension().height,stateObs, this.fescalaX, this.fescalaY);
        
        ancho = stateObs.getObservationGrid().length;
        alto = stateObs.getObservationGrid()[0].length;
    }


    private ArrayList<Vector2di> obtenListaGemasFaciles(StateObservation stateObs, ElapsedCpuTimer timer) {
    	ArrayList<Observation>[][] mundo = stateObs.getObservationGrid();
		ResolutorTareas resolutor_aux = new ResolutorTareas(mundo, mundo.length, mundo[0].length, stateObs, fescalaX, fescalaY);
		ArrayList<Vector2di> gemas_faciles = new ArrayList<Vector2di>();
		
		ArrayList<Observation>[] posiciones_gemas = stateObs.getResourcesPositions();
		ArrayList<Vector2di> gemas = new ArrayList<Vector2di>();
		for(Observation o : posiciones_gemas[0]) {
			Vector2di gema = new Vector2di();
			gema.x = (int) Math.round(o.position.x / fescalaX);
			gema.y = (int) Math.round(o.position.y / fescalaY);
			gemas.add(gema);
		}
		for(Vector2di gema : gemas) {
			resolutor_aux.reset();
			resolutor_aux.setParametros(stateObs);
			if(esGemaFacil(gema,resolutor_aux,mundo,timer))
				gemas_faciles.add(gema);
		}
		
		ArrayList<Vector2di> gemas9 = new ArrayList<Vector2di>();
		for(int i = 0; i < 9; ++i)
			gemas9.add(gemas_faciles.get(i));
		return gemas9;
	}

	private boolean esGemaFacil(Vector2di gema, ResolutorTareas resolutor_aux, ArrayList<Observation>[][] mundo, ElapsedCpuTimer timer) {
		boolean gema_facil = false;
		if(gema.x-1>=0) {
			gema_facil = mundo[gema.x][gema.y].size()==0;
		}
		if(!gema_facil) {
			for(int i = 0; i <= 25 && !gema_facil;i++)
				if(resolutor_aux.obtenCamino(gema.x, gema.y, timer,true).get(0)!=Types.ACTIONS.ACTION_NIL)
					gema_facil = true;
		}
		return gema_facil;
	}

	@Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	int col_start = (int) Math.round(stateObs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(stateObs.getAvatarPosition().y / fescalaY);
    	
    	resolutor.setParametros(stateObs);
    	
    	if(lista_acciones.size()==0)
    		resolutor.reset();
    	
    	if(lista_gemas_faciles.size()==0)
    		acabado=true;
    	
    	if(lista_acciones.size()==0 && lista_gemas_faciles.size()>0) {
    		if(col_start != lista_gemas_faciles.get(0).x || fila_start != lista_gemas_faciles.get(0).y) {    		
    			lista_acciones = resolutor.obtenCamino(lista_gemas_faciles.get(0).x, lista_gemas_faciles.get(0).y,elapsedTimer,false);
    			if(lista_acciones.size()==1 && lista_acciones.get(0)==Types.ACTIONS.ACTION_NIL)
    				lista_acciones.remove(0);
    				return Types.ACTIONS.ACTION_NIL;
    		}
    		else {
    			lista_gemas_faciles.remove(0);
    			resolutor.reset();
    		}
    	}
    	if(lista_acciones.size()>0) {
    		if(hayPeligroBicho(stateObs, lista_acciones)) {
    			System.out.println(lista_acciones);
    			System.out.println(stateObs.getAvatarOrientation());
    			System.out.println("Columna: " + col_start + ", Fila: " + fila_start);
    			lista_acciones = esquivaBicho(stateObs,lista_acciones);
    			System.out.println(lista_acciones);
    		}
	    	Types.ACTIONS accion = lista_acciones.get(0);
	    	lista_acciones.remove(0);
	    	return(accion);
    	}
    	if(acabado) {
    		lista_acciones = resolutor.salirPortal(elapsedTimer);
    	}
    	return Types.ACTIONS.ACTION_NIL;
    	
    }
    
    private boolean isAccesible(ArrayList<Observation>[][] mundo, int columna, int fila) {
		// Si el nodo está vacío es accesible
		boolean vacio = mundo[columna][fila].size()==0;
		if(!vacio) {
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
			// Si no hay un bicho ni un muro ni una piedra ni una piedra encima entonces es una casilla accesible
			boolean condicion = !bicho && !muro && !piedra && !piedra_arriba;
			System.out.println(condicion);
			return condicion;
		}
		System.out.println("Vacío");
		return true;
	}
    
    // Hay que controlar si la vía de escape está bloqueada para añadir una alternativa
    // Hasta ahora consigue deshacerse del bicho a veces si no se ve atrapado
    private ArrayList<ACTIONS> esquivaBicho(StateObservation obs,ArrayList<ACTIONS> lista_acciones2) {
    	ArrayList<Observation>[][] mundo = obs.getObservationGrid();
    	ArrayList<Types.ACTIONS> lista_acciones = new ArrayList<Types.ACTIONS>();
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	
		if(lista_acciones2.get(0)==Types.ACTIONS.ACTION_LEFT) {
			if(obs.getAvatarOrientation().x==1.0) {
				if(isAccesible(mundo, col_start+1, fila_start))
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				else if(isAccesible(mundo, col_start, fila_start+1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				}
				else if(isAccesible(mundo, col_start, fila_start-1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				}
			}
			else {
				if(isAccesible(mundo, col_start+1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				}
				else if(isAccesible(mundo, col_start, fila_start+1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				}
				else if(isAccesible(mundo, col_start, fila_start-1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				}
			}
    	}
		else if(lista_acciones2.get(0)==Types.ACTIONS.ACTION_RIGHT) {
			if(obs.getAvatarOrientation().x==-1.0) {
				if(isAccesible(mundo, col_start-1, fila_start))
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);	
				else if(isAccesible(mundo, col_start, fila_start-1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				}
				else if(isAccesible(mundo, col_start, fila_start+1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				}
			}
			else {
				if(isAccesible(mundo, col_start-1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
				}
				else if(isAccesible(mundo, col_start, fila_start-1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				}
				else if(isAccesible(mundo, col_start, fila_start+1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				}
			}
    	}
		else if(lista_acciones2.get(0)==Types.ACTIONS.ACTION_UP) {
			if(obs.getAvatarOrientation().y==1.0) {
				if(isAccesible(mundo, col_start, fila_start+1))
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				else if(isAccesible(mundo, col_start-1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
				}
				else if(isAccesible(mundo, col_start+1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				}
			}
			else {
				if(isAccesible(mundo, col_start, fila_start+1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
					lista_acciones.add(0,Types.ACTIONS.ACTION_DOWN);
				}
				else if(isAccesible(mundo, col_start-1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
				}
				else if(isAccesible(mundo, col_start+1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				}
			}
    	}
		else if(lista_acciones2.get(0)==Types.ACTIONS.ACTION_DOWN) {
			if(obs.getAvatarOrientation().y==-1.0) {
				if(isAccesible(mundo, col_start, fila_start-1))
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				else if(isAccesible(mundo, col_start-1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
				}
				else if(isAccesible(mundo, col_start+1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				}
			}
			else {
				if(isAccesible(mundo, col_start, fila_start-1)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
					lista_acciones.add(0,Types.ACTIONS.ACTION_UP);
				}	
				else if(isAccesible(mundo, col_start-1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_LEFT);
				}
				else if(isAccesible(mundo, col_start+1, fila_start)) {
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
					lista_acciones.add(0,Types.ACTIONS.ACTION_RIGHT);
				}
			}
		}
		return lista_acciones;
	}

	public boolean hayPeligroBicho(StateObservation obs, ArrayList<Types.ACTIONS> lista_acciones) {
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	
    	ArrayList<Observation>[][] mundo = obs.getObservationGrid();
    	
    	boolean hay_bicho = false;
    	
    	Types.ACTIONS accion = lista_acciones.get(0);
    	if(accion == Types.ACTIONS.ACTION_DOWN) {
    		if(fila_start+2<alto)
    			if(mundo[col_start][fila_start+2].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start][fila_start+2].get(0).itype==11 || mundo[col_start][fila_start+2].get(0).itype==10;
    		if(fila_start+1<alto && col_start-1>0)
    			if(mundo[col_start-1][fila_start+1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start-1][fila_start+1].get(0).itype==11 || mundo[col_start-1][fila_start+1].get(0).itype==10;
    		if(fila_start+1<alto && col_start+1<ancho)
    			if(mundo[col_start+1][fila_start+1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start+1][fila_start+1].get(0).itype==11 || mundo[col_start+1][fila_start+1].get(0).itype==10;
    	}
    	else if(accion == Types.ACTIONS.ACTION_UP) {
    		if(fila_start-2>0)
    			if(mundo[col_start][fila_start-2].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start][fila_start-2].get(0).itype==11 || mundo[col_start][fila_start-2].get(0).itype==10;
    		if(fila_start-1>0 && col_start-1>0)
    			if(mundo[col_start-1][fila_start-1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start-1][fila_start-1].get(0).itype==11 || mundo[col_start-1][fila_start-1].get(0).itype==10;
    		if(fila_start-1>0 && col_start+1<ancho)
    			if(mundo[col_start+1][fila_start-1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start+1][fila_start-1].get(0).itype==11 || mundo[col_start+1][fila_start-1].get(0).itype==10;
    	}
    	else if(accion == Types.ACTIONS.ACTION_LEFT) {
    		if(col_start-2>0)
    			if(mundo[col_start-2][fila_start].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start-2][fila_start].get(0).itype==11 || mundo[col_start-2][fila_start].get(0).itype==10;
    		if(col_start-1>0 && fila_start-1>0)
    			if(mundo[col_start-1][fila_start-1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start-1][fila_start-1].get(0).itype==11 || mundo[col_start-1][fila_start-1].get(0).itype==10;
    		if(col_start-1>0 && fila_start+1<alto)
    			if(mundo[col_start-1][fila_start+1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start-1][fila_start+1].get(0).itype==11 || mundo[col_start-1][fila_start+1].get(0).itype==10;
    	}
    	else if(accion == Types.ACTIONS.ACTION_RIGHT) {
    		if(col_start+2<ancho)
    			if(mundo[col_start+2][fila_start].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start+2][fila_start].get(0).itype==11 || mundo[col_start+2][fila_start].get(0).itype==10;
    		if(col_start+1<ancho && fila_start-1>0)
    			if(mundo[col_start+1][fila_start-1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start+1][fila_start-1].get(0).itype==11 || mundo[col_start+1][fila_start-1].get(0).itype==10;
    		if(col_start+1<ancho && fila_start+1<alto)
    			if(mundo[col_start+1][fila_start+1].size()!=0)
    				hay_bicho = hay_bicho || mundo[col_start+1][fila_start+1].get(0).itype==11 || mundo[col_start+1][fila_start+1].get(0).itype==10;
    	}
    	
    	return hay_bicho;
    }


}
