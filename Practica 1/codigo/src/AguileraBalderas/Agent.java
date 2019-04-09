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
import java.util.concurrent.ThreadLocalRandom;
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
    private ArrayList<Gema> lista_gemas_faciles;
    
    private ResolutorTareas resolutor;
    
    private boolean acabado;
    
    int alto, ancho;
    
    int fila_portal, col_portal;
    int fila_inicial, col_inicial;
    int tercer_punto_fila, tercer_punto_col;
    
    boolean escapando;
    
    int veces_escapadas;
    
    int bloqueado = 0;
    Gema gema_objetivo = null;
    
        
    
    private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
    
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
    	veces_escapadas=0;
    	escapando=false;
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
        
        lista_gemas_faciles = new ArrayList<Gema>();
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
        gema_objetivo = lista_gemas_faciles.get(0);
        
        resolutor = new ResolutorTareas(stateObs.getObservationGrid(), stateObs.getObservationGrid().length, stateObs.getObservationGrid()[0].length,stateObs, this.fescalaX, this.fescalaY);
        
        ancho = stateObs.getObservationGrid().length;
        alto = stateObs.getObservationGrid()[0].length;
        ArrayList<Observation>[][] mundo = stateObs.getObservationGrid();
        
        
        ArrayList<Vector2di> posiciones_accesibles = new ArrayList<Vector2di>();
        for(int i=0; i < ancho;++i)
        	for(int j = 0; j < alto; j++) {
        		resolutor.reset();
        		resolutor.setParametros(stateObs);
        		if(isAccesible(mundo, i, j)) {
	        		if(resolutor.obtenCamino(i, j, elapsedTimer, true).get(0)!=Types.ACTIONS.ACTION_NIL)
	        			posiciones_accesibles.add(new Vector2di(i,j));
        		}
        	}
        
        
        col_portal = (int) Math.round(stateObs.getPortalsPositions()[0].get(0).position.x / this.fescalaX);
    	fila_portal = (int) Math.round(stateObs.getPortalsPositions()[0].get(0).position.y / this.fescalaY);
    	
    	col_inicial = (int) Math.round(stateObs.getAvatarPosition().x / this.fescalaX);
    	fila_inicial = (int) Math.round(stateObs.getAvatarPosition().y / this.fescalaY);
    	
    	Vector2di max_dist = posiciones_accesibles.get(0);
        for(Vector2di pos : posiciones_accesibles) {
        	if(distanciaManhattan(max_dist.x, max_dist.y, col_inicial, fila_inicial)<distanciaManhattan(pos.x, pos.y, col_inicial, fila_inicial))
        		if(distanciaManhattan(max_dist.x, max_dist.y, col_portal, fila_portal)<distanciaManhattan(pos.x, pos.y, col_portal, fila_portal))
        			max_dist = pos;
        }
                
        tercer_punto_col = max_dist.x;
        tercer_punto_fila = max_dist.y;
    }


    private ArrayList<Gema> obtenListaGemasFaciles(StateObservation stateObs, ElapsedCpuTimer timer) {
    	int col_actual = (int) Math.round(stateObs.getAvatarPosition().x / this.fescalaX);
    	int fila_actual = (int) Math.round(stateObs.getAvatarPosition().y / this.fescalaY);
    	ArrayList<Observation>[][] mundo = stateObs.getObservationGrid();
		ResolutorTareas resolutor_aux = new ResolutorTareas(mundo, mundo.length, mundo[0].length, stateObs, fescalaX, fescalaY);
		ArrayList<Gema> gemas_faciles = new ArrayList<Gema>();
		ArrayList<Gema> gemas = new ArrayList<Gema>();
		
		ArrayList<Observation>[] posiciones_gemas = stateObs.getResourcesPositions();
		for(Observation o : posiciones_gemas[0]) {
			Gema gema = new Gema();
			gema.coordenadas.x = (int) Math.round(o.position.x / fescalaX);
			gema.coordenadas.y = (int) Math.round(o.position.y / fescalaY);
			gemas.add(gema);
		}
		ArrayList<Gema> gemas9 = new ArrayList<Gema>();
		while(gemas.size()>0) {
			gemas_faciles = new ArrayList<Gema>();
			for(Gema gema : gemas) {
				resolutor_aux.reset();
				resolutor_aux.setParametros(stateObs);
				for(int j = 0; j < 20; j++) {
					if(resolutor_aux.obtenCamino2(col_actual,fila_actual,gema.coordenadas.x, gema.coordenadas.y, timer,true).get(0)!=Types.ACTIONS.ACTION_NIL) {
						gema.distancia_actual = resolutor_aux.cantidad_pasos;
						gemas_faciles.add(gema);
					}
				}
			}
			if(gemas_faciles.size()>0) {
				Gema min = gemas_faciles.get(0);
				for(int j = 1; j<gemas_faciles.size();j++) {
					if(gemas_faciles.get(j).distancia_actual < min.distancia_actual)
						min = gemas_faciles.get(j);
				}
				gemas.remove(min);
				gemas9.add(min);
				col_actual = gemas9.get(gemas9.size()-1).coordenadas.x;
				fila_actual = gemas9.get(gemas9.size()-1).coordenadas.y;
			}
			else
				return gemas9;
			
		}
		return gemas9;
	}
    
	@Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
		//return Types.ACTIONS.ACTION_NIL;
    	int col_start = (int) Math.round(stateObs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(stateObs.getAvatarPosition().y / fescalaY);
    		
    	
    	resolutor.setParametros(stateObs);
    	if(lista_gemas_faciles.size()>0)
	    	if(gema_objetivo.equals(lista_gemas_faciles.get(0)))
	    		bloqueado+=1;
	    	else {
	    		gema_objetivo = lista_gemas_faciles.get(0);
	    		bloqueado=0;
	    	}
    	
    	if(bloqueado>150) {
    		Gema gem = lista_gemas_faciles.get(0);
    		lista_gemas_faciles.remove(0);
    		lista_gemas_faciles.add(gem);
    	}
    		
    	
    	if(stateObs.getAvatarResources().size()>0)
    		if(stateObs.getAvatarResources().get(6)==9)
    			acabado=true;
    	
    	if(lista_acciones.size()==0) {
    		resolutor.reset();
    		escapando=false;
    	}
    	
    	if(lista_gemas_faciles.size()==0)
    		acabado=true;
    	
    	if(lista_acciones.size()==0 && lista_gemas_faciles.size()>0) {
    		if(col_start != lista_gemas_faciles.get(0).coordenadas.x || fila_start != lista_gemas_faciles.get(0).coordenadas.y) {    		
    			lista_acciones = resolutor.obtenCamino(lista_gemas_faciles.get(0).coordenadas.x, lista_gemas_faciles.get(0).coordenadas.y,elapsedTimer,false);
    			if(lista_acciones.size()==1 && lista_acciones.get(0)==Types.ACTIONS.ACTION_NIL)
    				lista_acciones.remove(0);
    				stateObs.advance(Types.ACTIONS.ACTION_NIL);
    				return Types.ACTIONS.ACTION_NIL;
    		}
    		else {
    			lista_gemas_faciles.remove(0);
    			resolutor.reset();
    		}
    	}
    	if(lista_acciones.size()>0) {
    		if(this.escapando && hayPeligroBicho(stateObs, lista_acciones)) {
    			lista_acciones = escapaReactivo(stateObs, lista_acciones);
    		}
    		else if(hayPeligroBicho(stateObs, lista_acciones)) {
    			escapando=true;
    			lista_acciones = esquivaBicho(stateObs,lista_acciones, elapsedTimer);
    		}
    		if(lista_acciones.size()==0)
    			return Types.ACTIONS.ACTION_NIL;
	    	Types.ACTIONS accion = lista_acciones.get(0);
	    	stateObs.advance(accion);
	    	lista_acciones.remove(0);
	    	return(accion);
    	}
    	if(acabado) {
    		lista_acciones = resolutor.salirPortal(elapsedTimer);
    	}
    	stateObs.advance(Types.ACTIONS.ACTION_NIL);
    	return Types.ACTIONS.ACTION_NIL;
    	
    }
    
    private ArrayList<ACTIONS> escapaReactivo(StateObservation obs, ArrayList<ACTIONS> lista_acciones2) {
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
		else {
		//else if(lista_acciones2.get(0)==Types.ACTIONS.ACTION_DOWN) {
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
			// Comprueba si hay un monstruo arriba, abajo, a la izquierda o a la derecha
			boolean monstruo_alrededores = false;
			if(fila-1>=0)
				if(mundo[columna][fila-1].size()>0)
					monstruo_alrededores = monstruo_alrededores || mundo[columna][fila-1].get(0).itype==11 || mundo[columna][fila-1].get(0).itype==10;
			if(fila+1<alto)
				if(mundo[columna][fila+1].size()>0)
					monstruo_alrededores = monstruo_alrededores || mundo[columna][fila+1].get(0).itype==11 || mundo[columna][fila+1].get(0).itype==10;
			if(columna-1>=0)
				if(mundo[columna-1][fila].size()>0)
					monstruo_alrededores = monstruo_alrededores || mundo[columna-1][fila].get(0).itype==11 || mundo[columna-1][fila].get(0).itype==10;
			if(columna+1<ancho)
				if(mundo[columna+1][fila].size()>0)
					monstruo_alrededores = monstruo_alrededores || mundo[columna+1][fila].get(0).itype==11 || mundo[columna+1][fila].get(0).itype==10;
			// Si no hay un bicho ni un muro ni una piedra ni una piedra encima entonces es una casilla accesible
			boolean condicion = !bicho && !muro && !piedra && !piedra_arriba && !monstruo_alrededores;
			return condicion;
		}
		return true;
	}
    
    // Hay que controlar si la vía de escape está bloqueada para añadir una alternativa
    // Hasta ahora consigue deshacerse del bicho a veces si no se ve atrapado
    private ArrayList<ACTIONS> esquivaBicho(StateObservation obs,ArrayList<ACTIONS> lista_acciones2, ElapsedCpuTimer timer) {
    	this.veces_escapadas+=1;
    	ArrayList<Observation>[][] mundo = obs.getObservationGrid();
    	ArrayList<Types.ACTIONS> lista_acciones = new ArrayList<Types.ACTIONS>();
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	
    	resolutor.reset();
    	resolutor.setParametros(obs);
    	
    	int distancia_portal = distanciaManhattan(col_portal, fila_portal, col_start, fila_start);
    	int distancia_origen = distanciaManhattan(col_inicial, fila_inicial, col_start, fila_start);
    	int distancia_tercero = distanciaManhattan(tercer_punto_col, tercer_punto_fila, col_start, fila_start);
    	
    	if(distancia_portal<distancia_origen && distancia_portal<distancia_tercero)
    		lista_acciones = resolutor.obtenCamino(this.col_portal, this.fila_portal, timer, false);
    	else if(distancia_origen<distancia_portal && distancia_origen<distancia_tercero)
    		lista_acciones = resolutor.obtenCamino(this.tercer_punto_col, this.tercer_punto_fila, timer, false);
    	else
    		lista_acciones = resolutor.obtenCamino(this.col_inicial, this.fila_inicial, timer, false);
    	
    	
    	/*if(this.veces_escapadas%3==0)
    		lista_acciones = resolutor.obtenCamino(this.col_inicial, this.fila_inicial, timer, false);
    	else if(this.veces_escapadas%2==1)
    		lista_acciones = resolutor.obtenCamino(this.tercer_punto_col, this.tercer_punto_fila, timer, false);
    	else
    		lista_acciones = resolutor.obtenCamino(this.col_portal, this.fila_portal, timer, false);*/
    	return lista_acciones;
	}

	public boolean hayPeligroBicho(StateObservation obs, ArrayList<Types.ACTIONS> lista_acciones) {
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	
    	ArrayList<Observation>[][] mundo = obs.getObservationGrid();
    	
    	boolean hay_bicho = false;
    	
    	Types.ACTIONS accion = lista_acciones.get(0);
    	if(accion == Types.ACTIONS.ACTION_DOWN) {
    		ArrayList<Vector2di> posiciones = new ArrayList<Vector2di>();
    		if(fila_start+2>=0 && fila_start+2<ancho)
    			if(mundo[col_start][fila_start+2].size()>0) {
    				if(mundo[col_start][fila_start+2].get(0).itype!=0 && mundo[col_start][fila_start+2].get(0).itype!=7) {	
			    		posiciones.add(new Vector2di(col_start, fila_start+3));
			    		posiciones.add(new Vector2di(col_start, fila_start+2));
			    		posiciones.add(new Vector2di(col_start+1, fila_start+2));
			    		posiciones.add(new Vector2di(col_start-1, fila_start+2));
			    		posiciones.add(new Vector2di(col_start, fila_start+1));
			    		posiciones.add(new Vector2di(col_start-1, fila_start+1));
			    		posiciones.add(new Vector2di(col_start+1, fila_start+1));
			    		
			    		posiciones.add(new Vector2di(col_start-1, fila_start));
			    		posiciones.add(new Vector2di(col_start+1, fila_start));
			    		if(col_start+1<ancho)
			    			if(mundo[col_start+1][fila_start].size()>0) {
			    				if(mundo[col_start+1][fila_start].get(0).itype!=7 && mundo[col_start+1][fila_start].get(0).itype!=4 && mundo[col_start+1][fila_start].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start+2, fila_start));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start+2, fila_start));
			    		
			    		if(col_start-1>=0)
			    			if(mundo[col_start-1][fila_start].size()>0) {
			    				if(mundo[col_start-1][fila_start].get(0).itype!=7 && mundo[col_start-1][fila_start].get(0).itype!=4 && mundo[col_start-1][fila_start].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start-2, fila_start));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start-2, fila_start));
    				}
    			}
    			else {
    				posiciones.add(new Vector2di(col_start, fila_start+3));
		    		posiciones.add(new Vector2di(col_start, fila_start+2));
		    		posiciones.add(new Vector2di(col_start+1, fila_start+2));
		    		posiciones.add(new Vector2di(col_start-1, fila_start+2));
		    		posiciones.add(new Vector2di(col_start, fila_start+1));
		    		posiciones.add(new Vector2di(col_start-1, fila_start+1));
		    		posiciones.add(new Vector2di(col_start+1, fila_start+1));
		    		
		    		posiciones.add(new Vector2di(col_start-1, fila_start));
		    		posiciones.add(new Vector2di(col_start+1, fila_start));
		    		if(col_start+1<ancho)
		    			if(mundo[col_start+1][fila_start].size()>0) {
		    				if(mundo[col_start+1][fila_start].get(0).itype!=7 && mundo[col_start+1][fila_start].get(0).itype!=4 && mundo[col_start+1][fila_start].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start+2, fila_start));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start+2, fila_start));
		    		
		    		if(col_start-1>=0)
		    			if(mundo[col_start-1][fila_start].size()>0) {
		    				if(mundo[col_start-1][fila_start].get(0).itype!=7 && mundo[col_start-1][fila_start].get(0).itype!=4 && mundo[col_start-1][fila_start].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start-2, fila_start));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start-2, fila_start));
		    		
    			}
    		if(col_start-1>=0 && col_start-1<ancho && fila_start+1>=0 && fila_start+1<alto)
    			if(mundo[col_start-1][fila_start+1].size()!=0) {
    				if(mundo[col_start-1][fila_start+1].get(0).itype!=0 && mundo[col_start-1][fila_start+1].get(0).itype!=7 && mundo[col_start-1][fila_start+1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start-2, fila_start+1));
    			}
    			else
    				posiciones.add(new Vector2di(col_start-2, fila_start+1));
    		if(col_start+1>=0 && col_start+1<ancho && fila_start+1>=0 && fila_start+1<alto)
    			if(mundo[col_start+1][fila_start+1].size()!=0) {
    				if(mundo[col_start+1][fila_start+1].get(0).itype!=0 && mundo[col_start+1][fila_start+1].get(0).itype!=7 && mundo[col_start+1][fila_start+1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start+2, fila_start+1));
    			}
    			else
    				posiciones.add(new Vector2di(col_start+2, fila_start+1));
    		System.out.println("Estamos yendo hacia abajo");
    		for(Vector2di pos : posiciones)
    			if(pos.x>=0 && pos.x<ancho && pos.y>=0 && pos.y<alto)
    				if(mundo[pos.x][pos.y].size()!=0) {
    					hay_bicho = hay_bicho || mundo[pos.x][pos.y].get(0).itype==11 || mundo[pos.x][pos.y].get(0).itype==10;
    					System.out.println(mundo[pos.x][pos.y].get(0).itype);
    				}
    		System.out.println(hay_bicho);
    		System.out.println("\n\n");
    	}
    	else if(accion == Types.ACTIONS.ACTION_UP) {
    		ArrayList<Vector2di> posiciones = new ArrayList<Vector2di>();
    		if(fila_start-2>=0 && fila_start-2<ancho)
    			if(mundo[col_start][fila_start-2].size()>0) {
    				if(mundo[col_start][fila_start-2].get(0).itype!=0 && mundo[col_start][fila_start-2].get(0).itype!=7) {
    					posiciones.add(new Vector2di(col_start, fila_start-3));
    					posiciones.add(new Vector2di(col_start, fila_start-2));
    					posiciones.add(new Vector2di(col_start+1, fila_start-2));
    					posiciones.add(new Vector2di(col_start-1, fila_start-2));
    					posiciones.add(new Vector2di(col_start, fila_start-1));
    					posiciones.add(new Vector2di(col_start-1, fila_start-1));
    					posiciones.add(new Vector2di(col_start+1, fila_start-1));
    					
    					posiciones.add(new Vector2di(col_start-1, fila_start));
			    		posiciones.add(new Vector2di(col_start+1, fila_start));
			    		if(col_start+1<ancho)
			    			if(mundo[col_start+1][fila_start].size()>0) {
			    				if(mundo[col_start+1][fila_start].get(0).itype!=7 && mundo[col_start+1][fila_start].get(0).itype!=4 && mundo[col_start+1][fila_start].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start+2, fila_start));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start+2, fila_start));
			    		
			    		if(col_start-1>=0)
			    			if(mundo[col_start-1][fila_start].size()>0) {
			    				if(mundo[col_start-1][fila_start].get(0).itype!=7 && mundo[col_start-1][fila_start].get(0).itype!=4 && mundo[col_start-1][fila_start].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start-2, fila_start));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start-2, fila_start));
			    		
    				}
    			}
    			else {
    				posiciones.add(new Vector2di(col_start, fila_start-3));
					posiciones.add(new Vector2di(col_start, fila_start-2));
					posiciones.add(new Vector2di(col_start+1, fila_start-2));
					posiciones.add(new Vector2di(col_start-1, fila_start-2));
					posiciones.add(new Vector2di(col_start, fila_start-1));
					posiciones.add(new Vector2di(col_start-1, fila_start-1));
					posiciones.add(new Vector2di(col_start+1, fila_start-1));
					
					posiciones.add(new Vector2di(col_start-1, fila_start));
		    		posiciones.add(new Vector2di(col_start+1, fila_start));
		    		if(col_start+1<ancho)
		    			if(mundo[col_start+1][fila_start].size()>0) {
		    				if(mundo[col_start+1][fila_start].get(0).itype!=7 && mundo[col_start+1][fila_start].get(0).itype!=4 && mundo[col_start+1][fila_start].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start+2, fila_start));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start+2, fila_start));
		    		
		    		if(col_start-1>=0)
		    			if(mundo[col_start-1][fila_start].size()>0) {
		    				if(mundo[col_start-1][fila_start].get(0).itype!=7 && mundo[col_start-1][fila_start].get(0).itype!=4 && mundo[col_start-1][fila_start].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start-2, fila_start));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start-2, fila_start));
		    		
    			}
    		if(col_start-1>=0 && col_start-1<ancho && fila_start-1>=0 && fila_start-1<alto)
    			if(mundo[col_start-1][fila_start-1].size()!=0) {
    				if(mundo[col_start-1][fila_start-1].get(0).itype!=0 && mundo[col_start-1][fila_start-1].get(0).itype!=7 && mundo[col_start-1][fila_start-1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start-2, fila_start-1));
    			}
    			else
    				posiciones.add(new Vector2di(col_start-2, fila_start-1));
    		if(col_start+1>=0 && col_start+1<ancho && fila_start-1>=0 && fila_start-1<alto)
    			if(mundo[col_start+1][fila_start-1].size()!=0) {
    				if(mundo[col_start+1][fila_start-1].get(0).itype!=0 && mundo[col_start+1][fila_start-1].get(0).itype!=7 && mundo[col_start+1][fila_start-1].get(0).itype!=7)
    					posiciones.add(new Vector2di(col_start+2, fila_start-1));
    			}
    			else
    				posiciones.add(new Vector2di(col_start+2, fila_start-1));
    		System.out.println("Estamos yendo hacia arriba");
    		for(Vector2di pos : posiciones)
    			if(pos.x>=0 && pos.x<ancho && pos.y>=0 && pos.y<alto)
    				if(mundo[pos.x][pos.y].size()!=0) {
    					hay_bicho = hay_bicho || mundo[pos.x][pos.y].get(0).itype==11 || mundo[pos.x][pos.y].get(0).itype==10;
    					System.out.println(mundo[pos.x][pos.y].get(0).itype);
    				}
    		System.out.println(hay_bicho);
    		System.out.println("\n\n");
    	}
    	else if(accion == Types.ACTIONS.ACTION_LEFT) {
    		ArrayList<Vector2di> posiciones = new ArrayList<Vector2di>();
    		if(col_start-2>=0 && col_start-2<ancho)
    			if(mundo[col_start-2][fila_start].size()>0) {
    				if(mundo[col_start-2][fila_start].get(0).itype!=0 && mundo[col_start-2][fila_start].get(0).itype!=7) {
			    		posiciones.add(new Vector2di(col_start-3, fila_start));
			    		posiciones.add(new Vector2di(col_start-2, fila_start));
			    		posiciones.add(new Vector2di(col_start-2, fila_start-1));
			    		posiciones.add(new Vector2di(col_start-2, fila_start+1));
			    		posiciones.add(new Vector2di(col_start-1, fila_start));
			    		posiciones.add(new Vector2di(col_start-1, fila_start-1));
			    		posiciones.add(new Vector2di(col_start-1, fila_start+1));
			    		
			    		posiciones.add(new Vector2di(col_start, fila_start+1));
			    		posiciones.add(new Vector2di(col_start, fila_start-1));
			    		if(fila_start+1<alto)
			    			if(mundo[col_start][fila_start+1].size()>0) {
			    				if(mundo[col_start][fila_start+1].get(0).itype!=7 && mundo[col_start][fila_start+1].get(0).itype!=4 && mundo[col_start][fila_start+1].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start, fila_start+2));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start, fila_start+2));
			    		
			    		if(fila_start-1>=0)
			    			if(mundo[col_start][fila_start-1].size()>0) {
			    				if(mundo[col_start][fila_start-1].get(0).itype!=7 && mundo[col_start][fila_start-1].get(0).itype!=4 && mundo[col_start][fila_start-1].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start, fila_start-2));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start, fila_start-2));
    				}
    			}
    			else {
    				posiciones.add(new Vector2di(col_start-3, fila_start));
		    		posiciones.add(new Vector2di(col_start-2, fila_start));
		    		posiciones.add(new Vector2di(col_start-2, fila_start-1));
		    		posiciones.add(new Vector2di(col_start-2, fila_start+1));
		    		posiciones.add(new Vector2di(col_start-1, fila_start));
		    		posiciones.add(new Vector2di(col_start-1, fila_start-1));
		    		posiciones.add(new Vector2di(col_start-1, fila_start+1));
		    		
		    		posiciones.add(new Vector2di(col_start, fila_start+1));
		    		posiciones.add(new Vector2di(col_start, fila_start-1));
		    		if(fila_start+1<alto)
		    			if(mundo[col_start][fila_start+1].size()>0) {
		    				if(mundo[col_start][fila_start+1].get(0).itype!=7 && mundo[col_start][fila_start+1].get(0).itype!=4 && mundo[col_start][fila_start+1].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start, fila_start+2));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start, fila_start+2));
		    		
		    		if(fila_start-1>=0)
		    			if(mundo[col_start][fila_start-1].size()>0) {
		    				if(mundo[col_start][fila_start-1].get(0).itype!=7 && mundo[col_start][fila_start-1].get(0).itype!=4 && mundo[col_start][fila_start-1].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start, fila_start-2));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start, fila_start-2));
		    		
    			}
    		if(col_start-1>=0 && col_start-1<ancho && fila_start-1>=0 && fila_start-1<alto)
    			if(mundo[col_start-1][fila_start-1].size()!=0) {
    				if(mundo[col_start-1][fila_start-1].get(0).itype!=0 && mundo[col_start-1][fila_start-1].get(0).itype!=7 && mundo[col_start-1][fila_start-1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start-1, fila_start-2));
    			}
    			else
    				posiciones.add(new Vector2di(col_start-1, fila_start-2));
    		if(col_start-1>=0 && col_start-1<ancho && fila_start+1>=0 && fila_start+1<alto)
    			if(mundo[col_start-1][fila_start+1].size()!=0) {
    				if(mundo[col_start-1][fila_start+1].get(0).itype!=0 && mundo[col_start-1][fila_start+1].get(0).itype!=7 && mundo[col_start-1][fila_start+1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start-1, fila_start+2));
    			}
    			else
    				posiciones.add(new Vector2di(col_start-1, fila_start+2));
    		System.out.println("Estamos yendo hacia la izquierda");
    		for(Vector2di pos : posiciones)
    			if(pos.x>=0 && pos.x<ancho && pos.y>=0 && pos.y<alto)
    				if(mundo[pos.x][pos.y].size()!=0) {
    					hay_bicho = hay_bicho || mundo[pos.x][pos.y].get(0).itype==11 || mundo[pos.x][pos.y].get(0).itype==10;
    					System.out.println(mundo[pos.x][pos.y].get(0).itype);
    				}
    		System.out.println(hay_bicho);
    		System.out.println("\n\n");
    	}
    	else if(accion == Types.ACTIONS.ACTION_RIGHT) {
    		ArrayList<Vector2di> posiciones = new ArrayList<Vector2di>();
    		if(col_start+2>=0 && col_start+2<ancho)
    			if(mundo[col_start+2][fila_start].size()>0) {
    				if(mundo[col_start+2][fila_start].get(0).itype!=0 && mundo[col_start+2][fila_start].get(0).itype!=7) {
			    		posiciones.add(new Vector2di(col_start+3, fila_start));
			    		posiciones.add(new Vector2di(col_start+2, fila_start));
			    		posiciones.add(new Vector2di(col_start+2, fila_start-1));
			    		posiciones.add(new Vector2di(col_start+2, fila_start+1));
			    		posiciones.add(new Vector2di(col_start+1, fila_start));
			    		posiciones.add(new Vector2di(col_start+1, fila_start-1));
			    		posiciones.add(new Vector2di(col_start+1, fila_start+1));
			    		
			    		posiciones.add(new Vector2di(col_start, fila_start+1));
			    		posiciones.add(new Vector2di(col_start, fila_start-1));
			    		if(fila_start+1<alto)
			    			if(mundo[col_start][fila_start+1].size()>0) {
			    				if(mundo[col_start][fila_start+1].get(0).itype!=7 && mundo[col_start][fila_start+1].get(0).itype!=4 && mundo[col_start][fila_start+1].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start, fila_start+2));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start, fila_start+2));
			    		
			    		if(fila_start-1>=0)
			    			if(mundo[col_start][fila_start-1].size()>0) {
			    				if(mundo[col_start][fila_start-1].get(0).itype!=7 && mundo[col_start][fila_start-1].get(0).itype!=4 && mundo[col_start][fila_start-1].get(0).itype!=0)
			    					posiciones.add(new Vector2di(col_start, fila_start-2));
			    			}
			    			else
			    				posiciones.add(new Vector2di(col_start, fila_start-2));
			    		
    				}
    			}
    			else {
    				posiciones.add(new Vector2di(col_start+3, fila_start));
		    		posiciones.add(new Vector2di(col_start+2, fila_start));
		    		posiciones.add(new Vector2di(col_start+2, fila_start-1));
		    		posiciones.add(new Vector2di(col_start+2, fila_start+1));
		    		posiciones.add(new Vector2di(col_start+1, fila_start));
		    		posiciones.add(new Vector2di(col_start+1, fila_start-1));
		    		posiciones.add(new Vector2di(col_start+1, fila_start+1));
		    		
		    		posiciones.add(new Vector2di(col_start, fila_start+1));
		    		posiciones.add(new Vector2di(col_start, fila_start-1));
		    		if(fila_start+1<alto)
		    			if(mundo[col_start][fila_start+1].size()>0) {
		    				if(mundo[col_start][fila_start+1].get(0).itype!=7 && mundo[col_start][fila_start+1].get(0).itype!=4 && mundo[col_start][fila_start+1].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start, fila_start+2));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start, fila_start+2));
		    		
		    		if(fila_start-1>=0)
		    			if(mundo[col_start][fila_start-1].size()>0) {
		    				if(mundo[col_start][fila_start-1].get(0).itype!=7 && mundo[col_start][fila_start-1].get(0).itype!=4 && mundo[col_start][fila_start-1].get(0).itype!=0)
		    					posiciones.add(new Vector2di(col_start, fila_start-2));
		    			}
		    			else
		    				posiciones.add(new Vector2di(col_start, fila_start-2));
		    	
    			}
    		if(col_start+1>=0 && col_start+1<ancho && fila_start-1>=0 && fila_start-1<alto)
    			if(mundo[col_start+1][fila_start-1].size()!=0) {
    				if(mundo[col_start+1][fila_start-1].get(0).itype!=0 && mundo[col_start+1][fila_start-1].get(0).itype!=7 && mundo[col_start+1][fila_start-1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start+1, fila_start-2));
    			}
    			else
    				posiciones.add(new Vector2di(col_start+1, fila_start-2));
    		if(col_start+1>=0 && col_start+1<ancho && fila_start+1>=0 && fila_start+1<alto)
    			if(mundo[col_start+1][fila_start+1].size()!=0) {
    				if(mundo[col_start+1][fila_start+1].get(0).itype!=0 && mundo[col_start+1][fila_start+1].get(0).itype!=7 && mundo[col_start+1][fila_start+1].get(0).itype!=4)
    					posiciones.add(new Vector2di(col_start+1, fila_start+2));
    			}
    			else
    				posiciones.add(new Vector2di(col_start+1, fila_start+2));
    		System.out.println("Estamos yendo hacia la derecha");
    		for(Vector2di pos : posiciones)
    			if(pos.x>=0 && pos.x<ancho && pos.y>=0 && pos.y<alto)
    				if(mundo[pos.x][pos.y].size()!=0) {
    					hay_bicho = hay_bicho || mundo[pos.x][pos.y].get(0).itype==11 || mundo[pos.x][pos.y].get(0).itype==10;
    					System.out.println(mundo[pos.x][pos.y].get(0).itype);
    				}
    		System.out.println(hay_bicho);
    		System.out.println("\n\n");
    	}
    	
    	return hay_bicho;
    }


}
