package AguileraBalderas;

public class Gema {
	Vector2di coordenadas;
	int distancia_actual;
	//Es tipo 0 si está rodeada de piedras menos en la posición de arriba
	//Es de tipo 1 si tiene piedras arriba pero no en alguno de los dos lados
	int tipo_gema_piedra;
	
	public Gema() {
		this.coordenadas = new Vector2di(-1,-1);
		this.distancia_actual = -1;
	}
	
	@Override
	public String toString() {
		return "||" + coordenadas.toString() + ", Distancia actual: " + distancia_actual + "||";
	}
	
	@Override
    public boolean equals(Object arg0) {
        Gema gema = (Gema) arg0;
        return this.coordenadas.x == gema.coordenadas.x && this.coordenadas.y == gema.coordenadas.y;
	}
}
