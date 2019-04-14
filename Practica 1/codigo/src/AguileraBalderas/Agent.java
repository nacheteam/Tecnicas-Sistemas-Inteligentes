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


import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Paint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.RenderingHints.Key;
import java.awt.font.FontRenderContext;
import java.awt.font.GlyphVector;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ImageObserver;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.RenderableImage;
import java.text.AttributedCharacterIterator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Agent extends AbstractPlayer {
	//Factores de escala para conversión del mundo
    private int fescalaX;
    private int fescalaY;
    
    //Lista de acciones a realizar
    private ArrayList<Types.ACTIONS> lista_acciones;
    
    //Lista de gemas que se pueden coger si interaccionar con piedras ni bichos
    private ArrayList<Gema> lista_gemas_faciles;
    
    //Lista de gemas que se pueden coger fácil interaccionando con piedras
    private ArrayList<Gema> lista_gemas_faciles_piedras;
    
    //Lista de gemas que se pueden coger fácil interaccionando con bichos
    private ArrayList<Gema> lista_gemas_faciles_bichos;
    
    //Resolutor de tareas, nos da caminos y rutinas de acciones
    private ResolutorTareas resolutor;
    
    //Valor booleano que nos dice si hemos acabado la partida
    private boolean acabado;
    
    //Dimensiones del mundo
    int alto, ancho;
    
    //Coordenadas del portal, el punto inicial y un punto alejado a los dos primeros (se usan para escapar de forma planificada)
    int fila_portal, col_portal;
    int fila_inicial, col_inicial;
    int tercer_punto_fila, tercer_punto_col;
    
    //Valor booleano que nos indica si estamos escapando
    boolean escapando;
    
    //Número de veces que hemos escapado (para escoger de forma aleatoria el punto inicial, el portal o el tercer punto)
    int veces_escapadas;
    
    //Valor booleano para saber si estamos escpando de forma reactiva
    boolean escapando_reactivo;
    
    //Número de veces que hemos estado yendo a por la misma gema o que nos hemos quedado en la misma posicion, se usan para desbloquear al avatar
    int bloqueado = 0;
    //Gema objetivo, se usa para ver si llevamos mucho tiempo yendo a por la misma gema.
    Gema gema_objetivo = null;
    
    //Set de posiciones que no se pueden tocar porque hay bichos
    HashSet<Vector2di> contornos_bichos;
    
    StateObservation obs_draw;
    
    boolean primer_act = false;
            
    
    private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
    
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
    	this.primer_act=true;
    	for(int i = 0; i<100 ; ++i)
    		stateObs.advance(Types.ACTIONS.ACTION_NIL);
    	
    	this.contornos_bichos = new HashSet<Vector2di>();
    	
    	this.escapando_reactivo=false;
    	veces_escapadas=0;
    	escapando=false;
    	acabado = false;
 	
    	lista_acciones = new ArrayList<Types.ACTIONS>();

        this.fescalaX = stateObs.getWorldDimension().width / stateObs.getObservationGrid().length;
        this.fescalaY = stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length;
        
        resolutor = new ResolutorTareas(stateObs.getObservationGrid(), stateObs.getObservationGrid().length, stateObs.getObservationGrid()[0].length,stateObs, this.fescalaX, this.fescalaY);        
        this.contornos_bichos = resolutor.obtenRegionesBichos(stateObs);
        resolutor.reset();
        resolutor.setParametros(stateObs, this.contornos_bichos);
        
        lista_gemas_faciles = new ArrayList<Gema>();
        lista_gemas_faciles = obtenListaGemasFacilesSinBichos(stateObs,elapsedTimer);
        gema_objetivo = lista_gemas_faciles.get(0);
        
        ancho = stateObs.getObservationGrid().length;
        alto = stateObs.getObservationGrid()[0].length;
        ArrayList<Observation>[][] mundo = stateObs.getObservationGrid();
        
        
        ArrayList<Vector2di> posiciones_accesibles = new ArrayList<Vector2di>();
        for(int i=0; i < ancho;++i)
        	for(int j = 0; j < alto; j++) {
        		resolutor.reset();
        		resolutor.setParametros(stateObs, this.contornos_bichos);
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
        
        this.obs_draw = stateObs;
    }
    
    @Override
    public void draw(Graphics2D g) {
    	Debugger deb = new Debugger(this.obs_draw);
    	for(Vector2di v : this.contornos_bichos)
    		deb.drawCell(g, Color.red, v);
    }


    private ArrayList<Gema> obtenListaGemasFacilesSinBichos(StateObservation stateObs, ElapsedCpuTimer timer) {
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
			if(!contornos_bichos.contains(gema.coordenadas))
				gemas.add(gema);
		}
		ArrayList<Gema> gemas9 = new ArrayList<Gema>();
		while(gemas.size()>0) {
			gemas_faciles = new ArrayList<Gema>();
			for(Gema gema : gemas) {
				resolutor_aux.reset();
				resolutor_aux.setParametros(stateObs, this.contornos_bichos);
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
		try {
			Thread.sleep(500);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(lista_gemas_faciles);
		this.obs_draw = stateObs;
		
    	int col_start = (int) Math.round(stateObs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(stateObs.getAvatarPosition().y / fescalaY);
    	
    	resolutor.setParametros(stateObs, this.contornos_bichos);
    	this.contornos_bichos = resolutor.obtenRegionesBichos(stateObs);
    	if(lista_gemas_faciles.size()>0)
	    	if(gema_objetivo.equals(lista_gemas_faciles.get(0)))
	    		bloqueado+=1;
	    	else {
	    		gema_objetivo = lista_gemas_faciles.get(0);
	    		bloqueado=0;
	    	}
    	
    	
    	if(bloqueado>100) {
    		if(lista_gemas_faciles.size()>0) {
    			Gema gem = lista_gemas_faciles.get(0);
    			lista_gemas_faciles.remove(0);
    			resolutor.reset();
    			if(resolutor.obtenCamino(gem.coordenadas.x, gem.coordenadas.y, elapsedTimer, false).get(0)!=Types.ACTIONS.ACTION_NIL)
    				lista_gemas_faciles.add(gem);
    		}
    		bloqueado = 0;
    	}
    		
    	
    	if(stateObs.getAvatarResources().size()>0)
    		if(stateObs.getAvatarResources().get(6)==9)
    			acabado=true;
    	
    	if(lista_acciones.size()==0) {
    		this.escapando_reactivo=false;
    		resolutor.reset();
    		escapando=false;
    	}
    	
    	if(lista_gemas_faciles.size()==0)
    		acabado=true;
    	
    	if(lista_acciones.size()==0 && lista_gemas_faciles.size()>0) {
    		if(col_start != lista_gemas_faciles.get(0).coordenadas.x || fila_start != lista_gemas_faciles.get(0).coordenadas.y) {    		
    			lista_acciones = resolutor.obtenCamino(lista_gemas_faciles.get(0).coordenadas.x, lista_gemas_faciles.get(0).coordenadas.y,elapsedTimer,false);
    		}
    		else {
    			lista_gemas_faciles.remove(0);
    			resolutor.reset();
    		}
    	}
    	if(lista_acciones.size()>0) {
    		if(this.escapando && hayPeligroBicho(stateObs, lista_acciones) && !this.escapando_reactivo) {
    			this.escapando_reactivo = true;
    			lista_acciones = escapaReactivo(stateObs, lista_acciones);
    		}
    		/*else if(hayPeligroBicho(stateObs, lista_acciones) && !this.acabado) {
    			escapando=true;
    			lista_acciones = esquivaBicho(stateObs,lista_acciones, elapsedTimer);
    		}*/
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
			if(obs.getAvatarOrientation().y==-1.0) {
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
    	if(lista_acciones.size()>0) {
    	if(lista_acciones.get(0)==Types.ACTIONS.ACTION_RIGHT){
    		if(isAccesible(mundo, col_start+1, fila_start-1)) {
	    		lista_acciones.add(Types.ACTIONS.ACTION_UP);
	    		lista_acciones.add(Types.ACTIONS.ACTION_UP);
    		}
    	}
    	else if(lista_acciones.get(0)==Types.ACTIONS.ACTION_UP) {
    		if(isAccesible(mundo, col_start-1, fila_start-1)) {
    			lista_acciones.add(Types.ACTIONS.ACTION_LEFT);
    			lista_acciones.add(Types.ACTIONS.ACTION_LEFT);
    		}
    	}
    	else if(lista_acciones.get(0)==Types.ACTIONS.ACTION_LEFT) {
    		if(isAccesible(mundo, col_start-1, fila_start+1)) {
				lista_acciones.add(Types.ACTIONS.ACTION_DOWN);
				lista_acciones.add(Types.ACTIONS.ACTION_DOWN);
    		}
	    }
    	else {
	    	if(isAccesible(mundo, col_start+1, fila_start-1)) {	
	    		lista_acciones.add(Types.ACTIONS.ACTION_RIGHT);
	    		lista_acciones.add(Types.ACTIONS.ACTION_RIGHT);
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
    	ArrayList<Types.ACTIONS> lista_acciones = new ArrayList<Types.ACTIONS>();
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	
    	ArrayList<Vector2di> puntos_huida = new ArrayList<Vector2di>();
    	puntos_huida.add(new Vector2di(this.col_inicial, this.fila_inicial));
    	puntos_huida.add(new Vector2di(this.col_portal, this.fila_portal));
    	puntos_huida.add(new Vector2di(this.tercer_punto_col, this.tercer_punto_fila));
    	
    	resolutor.reset();
    	resolutor.setParametros(obs, this.contornos_bichos);
    	
    	boolean aleatorio = Math.random() < 0.25;
    	
    	if(aleatorio) {
	    	ArrayList<Integer> minima_distancia_bicho = new ArrayList<Integer>();
	    	    	
	    	for(Vector2di punto : puntos_huida) {
	    		int minima_distancia = Integer.MAX_VALUE;
	    		for(ArrayList<Observation> ob : obs.getNPCPositions(new Vector2d(col_start, fila_start))) {
	    			int col_bicho =(int) Math.round(ob.get(0).position.x / fescalaX);
	    			int fila_bicho =(int) Math.round(ob.get(0).position.y / fescalaY);
	    			if(distanciaManhattan(col_bicho, fila_bicho, punto.x, punto.y)<minima_distancia)
	    				minima_distancia = distanciaManhattan(col_bicho, fila_bicho, punto.x, punto.y);
	    		}
	    		minima_distancia_bicho.add(minima_distancia);
	    	}
	    	
	    	int maximo = 0;
	    	for(int i = 0; i < minima_distancia_bicho.size();++i)
	    		if(minima_distancia_bicho.get(i)>minima_distancia_bicho.get(maximo))
	    			maximo = i;
	    	
	    	lista_acciones = resolutor.obtenCamino(puntos_huida.get(maximo).x, puntos_huida.get(maximo).y, timer, false);
    	}
    	else {
			if(this.veces_escapadas%3==0)
				lista_acciones = resolutor.obtenCamino(this.col_inicial, this.fila_inicial, timer, false);
			else if(this.veces_escapadas%3==1)
				lista_acciones = resolutor.obtenCamino(this.tercer_punto_col, this.tercer_punto_fila, timer, false);
			else
				lista_acciones = resolutor.obtenCamino(this.col_portal, this.fila_portal, timer, false);
    	}
    	return lista_acciones;
	}
    
    public boolean noHayMuroPiedra(int col, int fila, ArrayList<Observation>[][] mundo) {
    	boolean no_muro_piedra = false;
    	if(col>=0 && fila>=0 && col<ancho && fila<alto) {
    		if(mundo[col][fila].size()>0) {
    			Observation ob = mundo[col][fila].get(0);
    			if(ob.itype!=7 && ob.itype!=0 /*&& ob.itype!=4*/)
    				no_muro_piedra=true;
    		}
    		else
    			no_muro_piedra=true;
    	}
    	return no_muro_piedra;
    }

    public boolean hayPeligroBicho(StateObservation obs, ArrayList<Types.ACTIONS> lista_acciones) {
    	int col_start = (int) Math.round(obs.getAvatarPosition().x / fescalaX);
    	int fila_start = (int) Math.round(obs.getAvatarPosition().y / fescalaY);
    	ArrayList<Observation>[][] mundo = obs.getObservationGrid();
    	
    	boolean hay_bicho = false;
    	ArrayList<Vector2di> posiciones = new ArrayList<Vector2di>();
    	
    	Types.ACTIONS accion = lista_acciones.get(0);
    	if(accion == Types.ACTIONS.ACTION_DOWN) {
    		
    		if(noHayMuroPiedra(col_start, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-1));
    		
    		if(noHayMuroPiedra(col_start, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+1));
    		if(noHayMuroPiedra(col_start-1, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start+1));
    		if(noHayMuroPiedra(col_start+1, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start+1));
    		
    		if(noHayMuroPiedra(col_start-1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start));
    		if(noHayMuroPiedra(col_start+1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start));
    		
    		//Comprobamos primero las casillas de la izquierda y derecha
    		if(posiciones.contains(new Vector2di(col_start-1, fila_start)) && noHayMuroPiedra(col_start-2, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-2, fila_start));
    		if(posiciones.contains(new Vector2di(col_start+1, fila_start)) && noHayMuroPiedra(col_start+2, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+2, fila_start));
    		
    		//Compruebo la primera y segunda diagonal
    		Vector2di cmenosuno_f = new Vector2di(col_start-1, fila_start);
    		Vector2di c_fmasuno = new Vector2di(col_start, fila_start+1);
    		Vector2di cmenosuno_fmasuno = new Vector2di(col_start-1, fila_start+1);
    		Vector2di cmasuno_f = new Vector2di(col_start+1, fila_start);
    		Vector2di cmasuno_fmasuno = new Vector2di(col_start+1, fila_start+1);
    		Vector2di c_fmasdos = new Vector2di(col_start, fila_start+2);
    		
    		if(((posiciones.contains(cmenosuno_f) && posiciones.contains(cmenosuno_fmasuno)) || (posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(c_fmasuno))) && noHayMuroPiedra(col_start-2, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start-2, fila_start+1));
    		if(((posiciones.contains(cmasuno_f) && posiciones.contains(cmasuno_fmasuno)) || (posiciones.contains(cmasuno_fmasuno) && posiciones.contains(c_fmasuno))) && noHayMuroPiedra(col_start+2, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start+2, fila_start+1));
    		
    		//Comprobamos dos posiciones enfrente
    		if(noHayMuroPiedra(col_start, fila_start+2, mundo) && posiciones.contains(new Vector2di(col_start, fila_start+1)))
    			posiciones.add(new Vector2di(col_start, fila_start+2));
 
    		//Comprobamos la segunda diagonal
    		if(noHayMuroPiedra(col_start-1, fila_start+2, mundo) && ((posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(cmenosuno_f)) || (posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(c_fmasuno)) || (posiciones.contains(c_fmasdos) && posiciones.contains(c_fmasuno))))
    			posiciones.add(new Vector2di(col_start-1, fila_start+2));
    		if(noHayMuroPiedra(col_start+1, fila_start+2, mundo) && ((posiciones.contains(cmasuno_fmasuno) && posiciones.contains(c_fmasuno)) || (posiciones.contains(cmasuno_fmasuno) && posiciones.contains(cmasuno_f)) || (posiciones.contains(c_fmasdos) && posiciones.contains(c_fmasuno))))
    			posiciones.add(new Vector2di(col_start+1, fila_start+2));
    		
    		//Comprobamos dos posiciones tres posiciones enfrente
    		if(noHayMuroPiedra(col_start, fila_start+3, mundo) && posiciones.contains(new Vector2di(col_start, fila_start+2)))
    			posiciones.add(new Vector2di(col_start, fila_start+3));
    		
    	}
    	else if(accion == Types.ACTIONS.ACTION_UP) {
    		if(noHayMuroPiedra(col_start, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+1));
    		
    		if(noHayMuroPiedra(col_start, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-1));
    		if(noHayMuroPiedra(col_start-1, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start-1));
    		if(noHayMuroPiedra(col_start+1, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start-1));
    		
    		if(noHayMuroPiedra(col_start-1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start));
    		if(noHayMuroPiedra(col_start+1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start));
    		
    		//Comprobamos primero las casillas de la izquierda y derecha
    		if(posiciones.contains(new Vector2di(col_start-1, fila_start)) && noHayMuroPiedra(col_start-2, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-2, fila_start));
    		if(posiciones.contains(new Vector2di(col_start+1, fila_start)) && noHayMuroPiedra(col_start+2, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+2, fila_start));
    		
    		//Compruebo la primera y segunda diagonal
    		Vector2di cmenosuno_f = new Vector2di(col_start-1, fila_start);
    		Vector2di c_fmenosuno = new Vector2di(col_start, fila_start-1);
    		Vector2di cmenosuno_fmenosuno = new Vector2di(col_start-1, fila_start-1);
    		Vector2di cmasuno_f = new Vector2di(col_start+1, fila_start);
    		Vector2di cmasuno_fmenosuno = new Vector2di(col_start+1, fila_start-1);
    		Vector2di c_fmenosdos = new Vector2di(col_start, fila_start-2);
    		
    		if(((posiciones.contains(cmenosuno_f) && posiciones.contains(cmenosuno_fmenosuno)) || (posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(c_fmenosuno))) && noHayMuroPiedra(col_start-2, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start-2, fila_start-1));
    		if(((posiciones.contains(cmasuno_f) && posiciones.contains(cmasuno_fmenosuno)) || (posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(c_fmenosuno))) && noHayMuroPiedra(col_start+2, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start+2, fila_start-1));
    		
    		//Comprobamos dos posiciones enfrente
    		if(noHayMuroPiedra(col_start, fila_start-2, mundo) && posiciones.contains(new Vector2di(col_start, fila_start-1)))
    			posiciones.add(new Vector2di(col_start, fila_start-2));
 
    		//Comprobamos la segunda diagonal
    		if(noHayMuroPiedra(col_start-1, fila_start-2, mundo) && ((posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(cmenosuno_f)) || (posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(c_fmenosuno)) || (posiciones.contains(c_fmenosdos) && posiciones.contains(c_fmenosuno))))
    			posiciones.add(new Vector2di(col_start-1, fila_start-2));
    		if(noHayMuroPiedra(col_start+1, fila_start-2, mundo) && ((posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(c_fmenosuno)) || (posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(cmasuno_f)) || (posiciones.contains(c_fmenosdos) && posiciones.contains(c_fmenosuno))))
    			posiciones.add(new Vector2di(col_start+1, fila_start-2));
    		
    		//Comprobamos dos posiciones tres posiciones enfrente
    		if(noHayMuroPiedra(col_start, fila_start-3, mundo) && posiciones.contains(new Vector2di(col_start, fila_start-2)))
    			posiciones.add(new Vector2di(col_start, fila_start-3));
    		
    	}
    	else if(accion == Types.ACTIONS.ACTION_LEFT) {
    		if(noHayMuroPiedra(col_start+1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start));
    		
    		if(noHayMuroPiedra(col_start-1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start));
    		if(noHayMuroPiedra(col_start-1, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start-1));
    		if(noHayMuroPiedra(col_start-1, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start+1));
    		
    		if(noHayMuroPiedra(col_start, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-1));
    		if(noHayMuroPiedra(col_start, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+1));
    		
    		//Comprobamos primero las casillas de la izquierda y derecha
    		if(posiciones.contains(new Vector2di(col_start, fila_start-1)) && noHayMuroPiedra(col_start, fila_start-2, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-2));
    		if(posiciones.contains(new Vector2di(col_start, fila_start+1)) && noHayMuroPiedra(col_start, fila_start+2, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+2));
    		
    		//Compruebo la primera y segunda diagonal
    		Vector2di c_fmenosuno = new Vector2di(col_start, fila_start-1);
    		Vector2di cmenosuno_f = new Vector2di(col_start-1, fila_start);
    		Vector2di cmenosuno_fmenosuno = new Vector2di(col_start-1, fila_start-1);
    		Vector2di c_fmasuno = new Vector2di(col_start, fila_start+1);
    		Vector2di cmenosuno_fmasuno = new Vector2di(col_start-1, fila_start+1);
    		Vector2di cmenosdos_f = new Vector2di(col_start-2, fila_start);
    		
    		if(((posiciones.contains(c_fmenosuno) && posiciones.contains(cmenosuno_fmenosuno)) || (posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(cmenosuno_f))) && noHayMuroPiedra(col_start-1, fila_start-2, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start-2));
    		if(((posiciones.contains(c_fmasuno) && posiciones.contains(cmenosuno_fmasuno)) || (posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(cmenosuno_f))) && noHayMuroPiedra(col_start-1, fila_start+2, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start+2));
    		
    		//Comprobamos dos posiciones enfrente
    		if(noHayMuroPiedra(col_start-2, fila_start, mundo) && posiciones.contains(new Vector2di(col_start-1, fila_start)))
    			posiciones.add(new Vector2di(col_start-2, fila_start));
 
    		//Comprobamos la segunda diagonal
    		if(noHayMuroPiedra(col_start-2, fila_start-1, mundo) && ((posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(c_fmenosuno)) || (posiciones.contains(cmenosuno_fmenosuno) && posiciones.contains(cmenosuno_f)) || (posiciones.contains(cmenosdos_f) && posiciones.contains(cmenosuno_f))))
    			posiciones.add(new Vector2di(col_start-2, fila_start-1));
    		if(noHayMuroPiedra(col_start-2, fila_start+1, mundo) && ((posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(cmenosuno_f)) || (posiciones.contains(cmenosuno_fmasuno) && posiciones.contains(c_fmasuno)) || (posiciones.contains(cmenosdos_f) && posiciones.contains(cmenosuno_f))))
    			posiciones.add(new Vector2di(col_start-2, fila_start+1));
    		
    		//Comprobamos dos posiciones tres posiciones enfrente
    		if(noHayMuroPiedra(col_start-3, fila_start, mundo) && posiciones.contains(new Vector2di(col_start-2, fila_start)))
    			posiciones.add(new Vector2di(col_start-3, fila_start));
    	}
    	else if(accion == Types.ACTIONS.ACTION_RIGHT) {
    		if(noHayMuroPiedra(col_start-1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start-1, fila_start));
    		
    		if(noHayMuroPiedra(col_start+1, fila_start, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start));
    		if(noHayMuroPiedra(col_start+1, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start-1));
    		if(noHayMuroPiedra(col_start+1, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start+1));
    		
    		if(noHayMuroPiedra(col_start, fila_start-1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-1));
    		if(noHayMuroPiedra(col_start, fila_start+1, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+1));
    		
    		//Comprobamos primero las casillas de la izquierda y derecha
    		if(posiciones.contains(new Vector2di(col_start, fila_start-1)) && noHayMuroPiedra(col_start, fila_start-2, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start-2));
    		if(posiciones.contains(new Vector2di(col_start, fila_start+1)) && noHayMuroPiedra(col_start, fila_start+2, mundo))
    			posiciones.add(new Vector2di(col_start, fila_start+2));
    		
    		//Compruebo la primera y segunda diagonal
    		Vector2di c_fmenosuno = new Vector2di(col_start, fila_start-1);
    		Vector2di cmasuno_f = new Vector2di(col_start+1, fila_start);
    		Vector2di cmasuno_fmenosuno = new Vector2di(col_start+1, fila_start-1);
    		Vector2di c_fmasuno = new Vector2di(col_start, fila_start+1);
    		Vector2di cmasuno_fmasuno = new Vector2di(col_start+1, fila_start+1);
    		Vector2di cmasdos_f = new Vector2di(col_start+2, fila_start);
    		
    		if(((posiciones.contains(c_fmenosuno) && posiciones.contains(cmasuno_fmenosuno)) || (posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(cmasuno_f))) && noHayMuroPiedra(col_start+1, fila_start-2, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start-2));
    		if(((posiciones.contains(c_fmasuno) && posiciones.contains(cmasuno_fmasuno)) || (posiciones.contains(cmasuno_fmasuno) && posiciones.contains(cmasuno_f))) && noHayMuroPiedra(col_start+1, fila_start+2, mundo))
    			posiciones.add(new Vector2di(col_start+1, fila_start+2));
    		
    		//Comprobamos dos posiciones enfrente
    		if(noHayMuroPiedra(col_start+2, fila_start, mundo) && posiciones.contains(new Vector2di(col_start+1, fila_start)))
    			posiciones.add(new Vector2di(col_start+2, fila_start));
 
    		//Comprobamos la segunda diagonal
    		if(noHayMuroPiedra(col_start+2, fila_start-1, mundo) && ((posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(c_fmenosuno)) || (posiciones.contains(cmasuno_fmenosuno) && posiciones.contains(cmasuno_f)) || (posiciones.contains(cmasdos_f) && posiciones.contains(cmasuno_f))))
    			posiciones.add(new Vector2di(col_start+2, fila_start-1));
    		if(noHayMuroPiedra(col_start+2, fila_start+1, mundo) && ((posiciones.contains(cmasuno_fmasuno) && posiciones.contains(cmasuno_f)) || (posiciones.contains(cmasuno_fmasuno) && posiciones.contains(c_fmasuno)) || (posiciones.contains(cmasdos_f) && posiciones.contains(cmasuno_f))))
    			posiciones.add(new Vector2di(col_start+2, fila_start+1));
    		
    		//Comprobamos dos posiciones tres posiciones enfrente
    		if(noHayMuroPiedra(col_start+3, fila_start, mundo) && posiciones.contains(new Vector2di(col_start+2, fila_start)))
    			posiciones.add(new Vector2di(col_start+3, fila_start));
    	}
    	
    	for(Vector2di pos : posiciones)
			if(pos.x>=0 && pos.x<ancho && pos.y>=0 && pos.y<alto)
				if(mundo[pos.x][pos.y].size()!=0) {
					hay_bicho = hay_bicho || mundo[pos.x][pos.y].get(0).itype==11 || mundo[pos.x][pos.y].get(0).itype==10;
				}
    	
    	return hay_bicho;
    }


}
