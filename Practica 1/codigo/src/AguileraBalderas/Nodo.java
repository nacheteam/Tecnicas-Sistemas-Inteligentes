package AguileraBalderas;


public class Nodo {
	public int coste_g;
	public int estimacion_h;
	public int f;
	public int fila;
	public int columna;
	public Nodo padre;
	
	public Nodo(int d,int e,int columna,int fila,Nodo n) {
		coste_g = d;
		estimacion_h = e;
		this.fila = fila;
		this.columna = columna;
		padre = n;
		f = coste_g + estimacion_h;
	}
	
	@Override
    public boolean equals(Object arg0) {
        Nodo nodo = (Nodo) arg0;
        return this.fila == nodo.fila && this.columna == nodo.columna;
	}
	
	@Override
	public String toString() {
		return "Fila: " + this.fila + ", Columna: " + this.columna;
	}
	
}
