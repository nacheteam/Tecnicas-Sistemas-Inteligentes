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
import tools.ElapsedCpuTimer;
import tools.Vector2d;
import tools.pathfinder.Node;
import tools.pathfinder.PathFinder;

import AguileraBalderas.AEstrella;

import javax.swing.*;
import java.util.ArrayList;
import java.util.HashMap;

public class Agent extends AbstractPlayer {
    //Objeto de clase Pathfinder
    private PathFinder pf;
    private int fescalaX;
    private int fescalaY;
    
    
    private int distanciaManhattan(int fila1, int col1, int fila2, int col2) {
		return Math.abs(fila1-fila2) + Math.abs(col1 - col2);
	}
    
    public Agent(StateObservation stateObs, ElapsedCpuTimer elapsedTimer) {
        //Creamos una lista de IDs de obstaculos
        ArrayList<Integer> tiposObs = new ArrayList();
        tiposObs.add(0); //<- Muros
        tiposObs.add(7); //<- Piedras

        //Se inicializa el objeto del pathfinder con las ids de los obstaculos
        pf = new PathFinder(tiposObs);
        pf.VERBOSE = false; // <- Activa o desactiva el modo la impresión del log

        //Se lanza el algoritmo de pathfinding para poder ser usado en la función ACT
        pf.run(stateObs);

        fescalaX = stateObs.getWorldDimension().width / stateObs.getObservationGrid().length;
        fescalaY = stateObs.getWorldDimension().height / stateObs.getObservationGrid()[0].length;
    }


    @Override
    public Types.ACTIONS act(StateObservation stateObs, ElapsedCpuTimer elapsedTimer){
    	int fila_start = (int) Math.round(stateObs.getAvatarPosition().x / fescalaX);
    	int col_start = (int) Math.round(stateObs.getAvatarPosition().y / fescalaY);
    	Nodo start = new Nodo(0, distanciaManhattan(fila_start, col_start, 1, 4), fila_start,col_start,null);
    	Nodo end = new Nodo(distanciaManhattan(1, 4, fila_start, col_start), 0, 1, 4, null);
    	
    	System.out.println(elapsedTimer.elapsedMillis());
    	AEstrella estrellica = new AEstrella(start, end, stateObs.getObservationGrid());
    	estrellica.buscaCamino(elapsedTimer);
    	if(estrellica.devuelveAcciones().size()!=0) {
    		System.out.println(estrellica.devuelveAcciones().get(0));
    		return estrellica.devuelveAcciones().get(0);
    	}
    	System.out.println(elapsedTimer.elapsedMillis());
    	return Types.ACTIONS.ACTION_NIL;
    }


}

